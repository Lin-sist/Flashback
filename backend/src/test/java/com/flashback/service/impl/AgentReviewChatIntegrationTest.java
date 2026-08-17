package com.flashback.service.impl;

import com.flashback.agent.AgentChatMode;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentConversationIntent;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
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
 * 友人回看对话端到端测试（C3b agent-review-chat）。
 *
 * 本类固定的是四件「不会自动成立」的事：
 * 1. 回看只能作用于 UNLOCKED 记录（DRAFT / SEALED 均拒绝）；
 * 2. 回看**不下发工具**——直接断言，不靠「恰好没配」；
 * 3. 回看**不产出可回填素材**；
 * 4. 回看对话不改动目标记录的任何字段。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentReviewChatIntegrationTest {

    private static final Long USER_ID = 9301L;
    private static final Long OTHER_USER_ID = 9302L;
    private static final Long UNLOCKED_ID = 5101L;
    private static final Long DRAFT_ID = 5102L;
    private static final Long SEALED_ID = 5103L;
    private static final String RECORD_CONTENT = "那时候项目截止日期压得我喘不过气，不知道还能不能撑住";

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AppAgentProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        insertUser(USER_ID, "review-user");
        insertUser(OTHER_USER_ID, "review-other");
        insertRecord(UNLOCKED_ID, USER_ID, "UNLOCKED");
        insertRecord(DRAFT_ID, USER_ID, "DRAFT");
        insertRecord(SEALED_ID, USER_ID, "SEALED");
    }

    private AgentSessionVO openReview(Long recordId) {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(recordId);
        request.setPurpose(AgentSessionPurpose.REVIEW_CHAT);
        return agentChatService.startOrResume(USER_ID, request);
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    // ---------- 记录状态范围 ----------

    @Test
    void shouldOpenReviewChatOnUnlockedRecord() {
        AgentSessionVO opened = openReview(UNLOCKED_ID);

        assertThat(opened.getStatus()).isEqualTo("SUCCESS");
        assertThat(opened.getStage()).isEqualTo("REVIEW");
        assertThat(opened.getConversationIntent()).isNull();
        assertThat(opened.getMessages()).hasSize(1);

        AgentSessionVO reloaded = agentChatService.getSession(USER_ID, opened.getSessionId());
        assertThat(reloaded.getConversationIntent())
                .as("REVIEW_CHAT 从数据库重读后仍不得获得写作 intent")
                .isNull();

        String purpose = jdbcTemplate.queryForObject(
                "SELECT purpose FROM agent_session WHERE id = ?", String.class, opened.getSessionId());
        assertThat(purpose).isEqualTo("REVIEW_CHAT");
    }

    @Test
    void shouldRejectReviewChatOnDraftRecord() {
        assertThatThrownBy(() -> openReview(DRAFT_ID))
                .hasMessageContaining("已解锁");
    }

    @Test
    void shouldRejectReviewChatOnSealedRecord() {
        // 封存未解锁的记录用户自己都还看不到，Agent 陪他聊它等于替时间拆封。
        assertThatThrownBy(() -> openReview(SEALED_ID))
                .hasMessageContaining("已解锁");
    }

    @Test
    void shouldRejectReviewChatWithoutRecord() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setPurpose(AgentSessionPurpose.REVIEW_CHAT);

        assertThatThrownBy(() -> agentChatService.startOrResume(USER_ID, request))
                .hasMessageContaining("需要指定记录");
    }

    @Test
    void shouldRejectWritingIntentOnReviewChat() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(UNLOCKED_ID);
        request.setPurpose(AgentSessionPurpose.REVIEW_CHAT);
        request.setConversationIntent(AgentConversationIntent.LISTEN);

        assertThatThrownBy(() -> agentChatService.startOrResume(USER_ID, request))
                .hasMessageContaining("不接受写作会话意图");
    }

    @Test
    void writingGuidanceMustStillRejectUnlockedRecord() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(UNLOCKED_ID);

        assertThatThrownBy(() -> agentChatService.startOrResume(USER_ID, request))
                .as("写作引导仍只允许草稿——引入回看不得放宽既有约束")
                .hasMessageContaining("草稿");
    }

    @Test
    void shouldRejectCrossUserReviewChat() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(UNLOCKED_ID);
        request.setPurpose(AgentSessionPurpose.REVIEW_CHAT);

        assertThatThrownBy(() -> agentChatService.startOrResume(OTHER_USER_ID, request))
                .hasMessageContaining("记录不存在");
    }

    // ---------- 无阶段机 ----------

    @Test
    void reviewChatMustNotAdvanceStagesOrReaskCount() {
        AgentSessionVO opened = openReview(UNLOCKED_ID);

        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在看好像没那么严重了"));
        AgentSessionVO afterSecond = agentChatService.sendMessage(
                USER_ID, opened.getSessionId(), message("嗯"));

        assertThat(afterSecond.getStage()).isEqualTo("REVIEW");
        Integer reask = jdbcTemplate.queryForObject(
                "SELECT stage_reask_count FROM agent_session WHERE id = ?",
                Integer.class, opened.getSessionId());
        assertThat(reask)
                .as("回看不追问，没有「同阶段再问一次」的概念")
                .isZero();
    }

    @Test
    void reviewChatShouldEndAtItsOwnTurnLimit() {
        properties.getReview().setMaxTurnsPerSession(2);
        try {
            AgentSessionVO opened = openReview(UNLOCKED_ID);
            agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在想起来还是有点感慨"));
            AgentSessionVO atLimit = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message("不过好像已经过去了"));

            assertThat(atLimit.getSessionStatus()).isEqualTo("ENDED");
        } finally {
            properties.getReview().setMaxTurnsPerSession(6);
        }
    }

    @Test
    void reviewChatShouldEndOnExplicitFinish() {
        AgentSessionVO opened = openReview(UNLOCKED_ID);
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在看好像没那么严重了"));

        AgentSessionVO finished = agentChatService.finish(USER_ID, opened.getSessionId());

        assertThat(finished.getSessionStatus()).isEqualTo("ENDED");
        assertThat(finished.getMaterialDraft())
                .as("回看主动结束同样不产素材")
                .isNull();
    }

    // ---------- 无工具、无素材（本刀最易漏的两处） ----------

    @Test
    void reviewChatMustNotProduceMaterial() {
        properties.getReview().setMaxTurnsPerSession(1);
        try {
            AgentSessionVO opened = openReview(UNLOCKED_ID);
            AgentSessionVO atLimit = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message("现在回头看，那时候太紧张了"));

            assertThat(atLimit.getSessionStatus()).isEqualTo("ENDED");
            assertThat(atLimit.getMaterialDraft())
                    .as("往已解锁记录追加此刻的整理会破坏它的时间完整性")
                    .isNull();
        } finally {
            properties.getReview().setMaxTurnsPerSession(6);
        }
    }

    @Test
    void reviewChatMustNotSurfaceToolProposals() {
        AgentSessionVO opened = openReview(UNLOCKED_ID);

        AgentSessionVO afterTurn = agentChatService.sendMessage(
                USER_ID, opened.getSessionId(), message("现在回头看，那时候太紧张了"));

        assertThat(afterTurn.getPendingToolCall()).isNull();
        Integer toolCalls = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_tool_call WHERE session_id = ?",
                Integer.class, opened.getSessionId());
        assertThat(toolCalls)
                .as("回看完全无工具：既不落待确认提议，也不留下待执行记录")
                .isZero();
    }

    /**
     * 这条守的是 tasks T-09 那个陷阱：`buildToolContext` 原先只按「有无 recordId」
     * 判断是否下发 tools，而回看会话**恰好绑定一条记录**。
     * 断言模式本身而非行为副作用，能在重构后依然抓住回归。
     */
    @Test
    void reviewModeMustDeclareToolsUnavailable() {
        assertThat(AgentChatMode.of(AgentSessionPurpose.REVIEW_CHAT).areToolsAvailable()).isFalse();
        assertThat(AgentChatMode.of(AgentSessionPurpose.REVIEW_CHAT).isMaterialProduced()).isFalse();
        assertThat(AgentChatMode.of(AgentSessionPurpose.REVIEW_CHAT).isStageMachineDriven()).isFalse();
        assertThat(AgentChatMode.of(AgentSessionPurpose.WRITING_GUIDANCE).areToolsAvailable()).isTrue();
    }

    // ---------- 记录不可变 ----------

    @Test
    void reviewChatMustLeaveRecordUntouched() {
        AgentSessionVO opened = openReview(UNLOCKED_ID);
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在看好像没那么严重了"));
        agentChatService.finish(USER_ID, opened.getSessionId());

        String content = jdbcTemplate.queryForObject(
                "SELECT content FROM `record` WHERE id = ?", String.class, UNLOCKED_ID);
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM `record` WHERE id = ?", String.class, UNLOCKED_ID);

        assertThat(content).isEqualTo(RECORD_CONTENT);
        assertThat(status).isEqualTo("UNLOCKED");
    }

    // ---------- 会话隔离 ----------

    @Test
    void reviewAndWritingSessionsMustNotCollideOnSameRecord() {
        // 同一条记录上人为造出两种用途的 ACTIVE 会话，验证恢复时不互相串。
        AgentSessionVO review = openReview(UNLOCKED_ID);
        jdbcTemplate.update("""
                INSERT INTO agent_session (user_id, record_id, purpose, stage, status, turn_count,
                                           stage_reask_count, last_active_at, created_at, updated_at)
                VALUES (?, ?, 'WRITING_GUIDANCE', 'EMOTION', 'ACTIVE', 0, 0, ?, ?, ?)
                """, USER_ID, UNLOCKED_ID, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        AgentSessionVO resumed = openReview(UNLOCKED_ID);

        assertThat(resumed.getSessionId())
                .as("回看恢复必须命中回看会话，而不是同记录上的写作会话")
                .isEqualTo(review.getSessionId());
    }

    // ---------- 数据准备 ----------

    private void insertUser(Long id, String username) {
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, ?, 'x', ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, id, username, username);
    }

    private void insertRecord(Long id, Long userId, String status) {
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, title, content, status, record_type,
                                      ai_summary, belief_then, created_at, updated_at)
                VALUES (?, ?, '那段日子', ?, ?, 'NODE_RECORD', ?, ?, ?, ?)
                """,
                id, userId, RECORD_CONTENT, status,
                "那时在为项目排期焦虑", "我以为撑不过去",
                LocalDateTime.of(2026, 3, 14, 21, 0), LocalDateTime.of(2026, 3, 14, 21, 0));
    }
}
