package com.flashback.service.impl;

import com.flashback.agent.tool.AgentToolCallStatus;
import com.flashback.agent.tool.AgentToolDecision;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentToolCall;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.AgentToolCallMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.UserMapper;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * C2 工具调用端到端集成测试（真实 H2 + MyBatis + mock provider，零外调）。
 *
 * 覆盖核心链路：提议 → 确认 → 执行 → 结果回注；以及拒绝、幂等、越权、封存后失败。
 *
 * 说明：mock provider 伪造的 tool_calls 形状与真实 provider 一致，
 * 因此走的是同一条解析 → 校验 → 落库路径，而非特设测试分支。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentToolCallingIntegrationTest {

    private static final Long TAG_ID = 9901L;

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AgentToolCallMapper agentToolCallMapper;

    @Autowired
    private AgentSessionMapper agentSessionMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long strangerId;
    private Long recordId;

    @BeforeEach
    void setUp() {
        userId = createUser("tool-owner");
        strangerId = createUser("tool-stranger");
        recordId = createDraft("已经写下的一点正文");
        jdbcTemplate.update(
                "INSERT INTO tag (id, name, type, status, created_at) VALUES (?, ?, ?, ?, ?)",
                TAG_ID, "工作焦虑", "MOOD", "ENABLED", LocalDateTime.now());
    }

    // ---------- 提议 ----------

    /**
     * mock provider 在 CORE_QUESTION 阶段提议追加正文。
     * 提议只落库为待确认，且此时记录正文必须完全不变。
     */
    @Test
    void shouldPersistProposalWithoutExecuting() {
        Long sessionId = startSession();
        AgentSessionVO vo = advanceToProposal(sessionId);

        assertThat(vo.getPendingToolCall()).isNotNull();
        assertThat(vo.getPendingToolCall().getTool()).isEqualTo("append_record_content");
        assertThat(vo.getPendingToolCall().getStatus()).isEqualTo("PROPOSED");
        assertThat(vo.getPendingToolCall().getAskText()).isNotBlank();

        // 关键：提议阶段绝不写记录。
        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent())
                .isEqualTo("已经写下的一点正文");
    }

    @Test
    void shouldNotStoreRawTextInAuditDigest() {
        Long sessionId = startSession();
        AgentSessionVO vo = advanceToProposal(sessionId);

        AgentToolCall toolCall = agentToolCallMapper.selectByIdAndUserId(
                vo.getPendingToolCall().getToolCallId(), userId);

        // 审计摘要只含长度与哈希前缀，不含原文（design 决策 6）。
        assertThat(toolCall.getArgsDigest()).contains("len=", "sha256=");
        assertThat(toolCall.getArgsDigest()).doesNotContain("撑不住");
    }

    // ---------- 确认执行 ----------

    @Test
    void shouldExecuteAppendOnAcceptAndKeepExistingContent() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        AgentSessionVO vo = agentChatService.confirmToolCall(
                userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);

        assertThat(vo.getLastToolCallResult().getStatus()).isEqualTo("EXECUTED");
        assertThat(vo.getPendingToolCall()).isNull();

        String content = recordMapper.selectByIdAndUserId(recordId, userId).getContent();
        // 原文逐字保留在前，新内容追加在后。
        assertThat(content).startsWith("已经写下的一点正文");
        assertThat(content.length()).isGreaterThan("已经写下的一点正文".length());
    }

    /**
     * 提议一经终结，瞬态执行参数必须被清空——审计表不留日记文本的长期副本。
     */
    @Test
    void shouldClearPendingArgsAfterSettlement() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        agentChatService.confirmToolCall(userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);

        AgentToolCall settled = agentToolCallMapper.selectByIdAndUserId(toolCallId, userId);
        assertThat(settled.getPendingArgs()).isNull();
    }

    @Test
    void shouldLeaveRecordUnchangedOnReject() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        AgentSessionVO vo = agentChatService.confirmToolCall(
                userId, sessionId, toolCallId, AgentToolDecision.REJECT);

        assertThat(vo.getLastToolCallResult().getStatus()).isEqualTo("REJECTED");
        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent())
                .isEqualTo("已经写下的一点正文");
    }

    // ---------- 幂等 ----------

    /**
     * 重复确认不得重复执行——否则用户点两次就会出现两段重复正文。
     */
    @Test
    void shouldBeIdempotentOnRepeatedAccept() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        agentChatService.confirmToolCall(userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);
        String afterFirst = recordMapper.selectByIdAndUserId(recordId, userId).getContent();

        AgentSessionVO second = agentChatService.confirmToolCall(
                userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);

        assertThat(second.getLastToolCallResult().getStatus()).isEqualTo("EXECUTED");
        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent())
                .isEqualTo(afterFirst);
    }

    @Test
    void shouldNotExecuteAfterReject() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        agentChatService.confirmToolCall(userId, sessionId, toolCallId, AgentToolDecision.REJECT);
        agentChatService.confirmToolCall(userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);

        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent())
                .isEqualTo("已经写下的一点正文");
        assertThat(agentToolCallMapper.selectByIdAndUserId(toolCallId, userId).getStatus())
                .isEqualTo(AgentToolCallStatus.REJECTED);
    }

    // ---------- 封存后失败 ----------

    /**
     * 提议之后记录被封存：确认时执行被拒，显式失败，不谎报成功。
     */
    @Test
    void shouldFailExplicitlyWhenRecordSealedAfterProposal() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();
        sealRecord();

        AgentSessionVO vo = agentChatService.confirmToolCall(
                userId, sessionId, toolCallId, AgentToolDecision.ACCEPT);

        assertThat(vo.getStatus()).isEqualTo("FAILED");
        assertThat(vo.getLastToolCallResult().getStatus()).isEqualTo("FAILED");
        assertThat(vo.getLastToolCallResult().getFailureType()).isEqualTo("business-rejected");
        assertThat(vo.getMessage()).contains("封存");
    }

    // ---------- 越权 ----------

    @Test
    void shouldRejectConfirmFromOtherUser() {
        Long sessionId = startSession();
        Long toolCallId = advanceToProposal(sessionId).getPendingToolCall().getToolCallId();

        assertThatThrownBy(() -> agentChatService.confirmToolCall(
                strangerId, sessionId, toolCallId, AgentToolDecision.ACCEPT))
                .isInstanceOf(NotFoundException.class);

        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent())
                .isEqualTo("已经写下的一点正文");
    }

    @Test
    void shouldRejectUnknownToolCallId() {
        Long sessionId = startSession();

        assertThatThrownBy(() -> agentChatService.confirmToolCall(
                userId, sessionId, 99999999L, AgentToolDecision.ACCEPT))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- 会话进度不受影响 ----------

    /**
     * 工具确认不推进阶段、不增加轮次（design 决策 8）。
     */
    @Test
    void shouldNotAdvanceStageOrTurnOnConfirm() {
        Long sessionId = startSession();
        AgentSessionVO proposed = advanceToProposal(sessionId);
        int turnBefore = proposed.getTurnCount();
        String stageBefore = proposed.getStage();

        AgentSessionVO vo = agentChatService.confirmToolCall(
                userId, sessionId, proposed.getPendingToolCall().getToolCallId(), AgentToolDecision.ACCEPT);

        assertThat(vo.getTurnCount()).isEqualTo(turnBefore);
        assertThat(vo.getStage()).isEqualTo(stageBefore);

        AgentSession session = agentSessionMapper.selectByIdAndUserId(sessionId, userId);
        assertThat(session.getTurnCount()).isEqualTo(turnBefore);
    }

    // ---------- helpers ----------

    private Long startSession() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(recordId);
        return agentChatService.startOrResume(userId, request).getSessionId();
    }

    /**
     * 推进到 mock provider 会给出提议的阶段（CORE_QUESTION）。
     */
    private AgentSessionVO advanceToProposal(Long sessionId) {
        AgentSessionVO vo = null;
        for (int i = 0; i < 4; i++) {
            vo = agentChatService.sendMessage(userId, sessionId, message("工作上有点撑不住了" + i));
            if (vo.getPendingToolCall() != null) {
                return vo;
            }
        }
        throw new IllegalStateException("mock provider 未产生工具提议");
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    private void sealRecord() {
        LocalDateTime now = LocalDateTime.now();
        recordMapper.updateDraftUnlockAtByIdAndUserId(recordId, userId, now.plusYears(1), now);
        recordMapper.sealDraftByIdAndUserId(recordId, userId, now, now);
    }

    private Long createUser(String suffix) {
        long unique = System.nanoTime();
        User user = new User();
        user.setUsername(suffix + "_" + unique);
        user.setPasswordHash("hash_" + unique);
        user.setNickname(suffix);
        user.setEmail(suffix + unique + "@test.com");
        user.setOpenid("openid-" + suffix + "-" + unique);
        user.setStatus(UserStatus.ENABLED);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user.getId();
    }

    private Long createDraft(String content) {
        Record record = new Record();
        record.setUserId(userId);
        record.setTitle("测试草稿");
        record.setContent(content);
        record.setRecordType(RecordType.EMOTION_NOTE);
        record.setStatus(RecordStatus.DRAFT);
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        recordMapper.insert(record);
        return record.getId();
    }
}
