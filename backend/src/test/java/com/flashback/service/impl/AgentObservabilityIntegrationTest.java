package com.flashback.service.impl;

import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentTurnTrace;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentTurnTraceMapper;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 决策轨迹端到端测试（C5 agent-observability）。
 *
 * <h3>为什么本类不加 {@code @Transactional}</h3>
 * {@code AgentTraceSink.persist} 走 {@code REQUIRES_NEW}（design 决策 7）——
 * 它必须独立于对话事务，否则痕迹写入失败会把用户消息一起回滚。
 * 若测试方法包在一个未提交的事务里，独立事务读不到测试插入的 fixture（外键失败），
 * 因此本类手动清理数据，不依赖回滚。
 *
 * 本类固定的是四件不会自动成立的事：
 * 1. 每轮留下一条轨迹，且**早退路径也留**（provider 不可用、护栏降级）；
 * 2. 同轮重试可辨识（attemptNo 递增），不被误记为新的一轮；
 * 3. 轨迹里**没有一个字**的日记原文 / 对话原文 / 记忆片段（T-25，本刀最硬的一条）；
 * 4. 开关关闭时行为等价于引入 C5 之前。
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentObservabilityIntegrationTest {

    private static final Long USER_ID = 9601L;
    private static final Long DRAFT_ID = 5601L;
    private static final Long UNLOCKED_ID = 5602L;

    /**
     * 特征串：只要它出现在任何轨迹字段里，就说明原文泄漏了。
     * 刻意选一个不会被规则词表或结构化标识意外包含的字符串。
     */
    private static final String SECRET_MARKER = "紫罗兰色的旧铁皮盒子";
    private static final String RECORD_CONTENT = "那时候我把 " + SECRET_MARKER + " 藏在抽屉最里面";
    private static final String USER_UTTERANCE = "今天又想起 " + SECRET_MARKER + " 那件事了";

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AgentTurnTraceMapper agentTurnTraceMapper;

    @Autowired
    private AppAgentProperties properties;

    @Autowired
    private AppAiProperties appAiProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
        insertUser(USER_ID, "trace-user");
        insertRecord(DRAFT_ID, USER_ID, "DRAFT");
        insertRecord(UNLOCKED_ID, USER_ID, "UNLOCKED");
    }

    // ---------- 轨迹完整性 ----------

    @Test
    void shouldRecordOneTracePerTurn() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(opened.getSessionId());

        assertThat(traces)
                .as("开场不算一轮（turnNo=0 且无配对用户消息），只有真实轮次留痕")
                .hasSize(1);
        AgentTurnTrace trace = traces.get(0);
        assertThat(trace.getTurnNo()).isEqualTo(1);
        assertThat(trace.getAttemptNo()).isEqualTo(1);
        assertThat(trace.getPurpose()).isEqualTo("WRITING_GUIDANCE");
        assertThat(trace.getOutcome()).isEqualTo("SUCCESS");
        assertThat(trace.getTraceId()).hasSize(32);
    }

    @Test
    void traceShouldCarryThoughtActionObservationSegments() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        AgentTurnTrace trace = onlyTrace(opened.getSessionId());
        String steps = trace.getStepsJson();

        // thought
        assertThat(steps).contains("\"step\":\"mode\"");
        assertThat(steps).contains("\"step\":\"stage-decision\"");
        assertThat(steps).contains("\"step\":\"memory-retrieval\"");
        assertThat(trace.getStageReason())
                .as("复用既有 AgentStageDecision.Reason，不另造一套阶段语义")
                .isNotBlank();
        // action
        assertThat(steps).contains("\"step\":\"provider\"");
        assertThat(trace.getProviderDurationMs())
                .as("成功路径的耗时不再被丢弃（C5 前只有失败路径算耗时）")
                .isNotNull();
        assertThat(steps).contains("\"step\":\"tools\"");
        // observation
        assertThat(steps).contains("\"step\":\"guardrail\"");
        assertThat(steps).contains("reply-content");
        assertThat(steps).contains("reply-attribution");
    }

    @Test
    void traceShouldCarryVersionAnchors() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        AgentTurnTrace trace = onlyTrace(opened.getSessionId());

        assertThat(trace.getPromptVersion()).startsWith("p").hasSize(9);
        assertThat(trace.getPolicyVersion()).startsWith("g").hasSize(9);
        assertThat(trace.getModel()).isNotBlank();
    }

    @Test
    void reviewChatTurnShouldBeDistinguishableInTrace() {
        AgentSessionVO opened = openReview();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在看好像没那么重了"));

        AgentTurnTrace trace = onlyTrace(opened.getSessionId());

        assertThat(trace.getPurpose()).isEqualTo("REVIEW_CHAT");
        assertThat(trace.getStage()).isEqualTo("REVIEW");
        assertThat(trace.getStageReason())
                .as("回看无阶段机，不伪造一个不存在的判定结论")
                .isNull();
        assertThat(trace.getStepsJson()).contains("\"step\":\"stage-retained\"");
    }

    // ---------- 早退路径 ----------

    @Test
    void shouldRecordTraceWhenProviderUnavailable() {
        AgentSessionVO opened = openWriting();
        properties.getMemory().setEnabled(true);
        boolean mockWasEnabled = setMockEnabled(false);
        try {
            AgentSessionVO result = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

            assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");
            AgentTurnTrace trace = onlyTrace(opened.getSessionId());
            assertThat(trace.getOutcome()).isEqualTo("UNAVAILABLE");
            assertThat(trace.getCauseType()).isEqualTo("provider-unavailable");
        } finally {
            setMockEnabled(mockWasEnabled);
        }
    }

    /**
     * 同轮重试：provider 先不可用，用户重发原消息后成功。
     *
     * 这条守的是 attemptNo 的存在意义——(sessionId, turnNo) 在重试时重复，
     * 没有 attemptNo 就分不清「重试」与「新一轮」。
     */
    @Test
    void retriedTurnShouldBeDistinguishableFromNewTurn() {
        AgentSessionVO opened = openWriting();
        boolean mockWasEnabled = setMockEnabled(false);
        try {
            agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));
        } finally {
            setMockEnabled(mockWasEnabled);
        }
        // 同一轮重试原消息
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(opened.getSessionId());

        assertThat(traces).hasSize(2);
        assertThat(traces).allSatisfy(t -> assertThat(t.getTurnNo()).isEqualTo(1));
        assertThat(traces.get(0).getAttemptNo()).isEqualTo(1);
        assertThat(traces.get(1).getAttemptNo()).isEqualTo(2);
        assertThat(traces.get(0).getOutcome()).isEqualTo("UNAVAILABLE");
        assertThat(traces.get(1).getOutcome()).isEqualTo("SUCCESS");
        assertThat(traces.get(1).getStepsJson())
                .as("重试不再次推进阶段机")
                .contains("\"step\":\"stage-retained\"");
    }

    /**
     * 长度硬上限生效时留痕。
     *
     * 裁剪不算降级（内容仍是 provider 产出），但排查「Agent 的话怎么断在半句」时
     * 这一条是直接答案。
     *
     * 降级方向与 V4/V5 的会话关联由 {@code AgentGuardrailTraceCorrelationTest} 覆盖——
     * mock provider 的回复本身是合规的，在集成层强行制造违规回复不现实。
     */
    @Test
    void shouldRecordReplyClippingWhenLengthLimitApplies() {
        AgentSessionVO opened = openWriting();
        int originalMax = properties.getMaxReplyChars();
        properties.setMaxReplyChars(4);
        try {
            agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

            AgentTurnTrace trace = onlyTrace(opened.getSessionId());
            assertThat(trace.getStepsJson()).contains("\"step\":\"reply-clipped\"");
            assertThat(trace.getStepsJson()).contains("reply-length");
        } finally {
            properties.setMaxReplyChars(originalMax);
        }
    }

    // ---------- 隐私（本刀最硬的一条，T-25）----------

    @Test
    void traceMustNotContainAnyDiaryOrConversationContent() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(opened.getSessionId());
        assertThat(traces).isNotEmpty();

        for (AgentTurnTrace trace : traces) {
            assertThat(everyTextField(trace))
                    .as("轨迹的任何字段都不得出现日记正文 / 对话原文的片段")
                    .doesNotContain(SECRET_MARKER);
        }
        // 直接从数据库再查一遍，防止「实体没带但落库带了」这种映射级泄漏。
        List<String> rawColumns = jdbcTemplate.queryForList(
                "SELECT CONCAT_WS('|', purpose, stage, stage_reason, model, prompt_version, "
                        + "policy_version, outcome, cause_type, downgrade_path, violation, steps_json) "
                        + "FROM agent_turn_trace WHERE session_id = ?",
                String.class, opened.getSessionId());
        assertThat(rawColumns).isNotEmpty();
        assertThat(rawColumns).allSatisfy(row -> assertThat(row).doesNotContain(SECRET_MARKER));
    }

    @Test
    void reviewTraceMustNotContainRecordContent() {
        // 回看会把记录正文注入 MEMORY 层，是原文最容易漏进轨迹的路径。
        AgentSessionVO opened = openReview();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("现在看好像没那么重了"));

        for (AgentTurnTrace trace : agentTurnTraceMapper.selectBySessionId(opened.getSessionId())) {
            assertThat(everyTextField(trace)).doesNotContain(SECRET_MARKER);
        }
    }

    @Test
    void sessionResponseMustNotExposeTrace() {
        AgentSessionVO opened = openWriting();
        AgentSessionVO result = agentChatService.sendMessage(
                USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        // 产品 API 不返回轨迹：AgentSessionVO 上不存在任何轨迹字段。
        assertThat(result.getClass().getDeclaredFields())
                .noneSatisfy(field -> assertThat(field.getName().toLowerCase()).contains("trace"));
    }

    // ---------- 开关与 fail-open ----------

    @Test
    void disabledObservabilityShouldBehaveAsBeforeC5() {
        properties.getObservability().setEnabled(false);
        try {
            AgentSessionVO opened = openWriting();
            AgentSessionVO result = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

            assertThat(result.getStatus())
                    .as("关闭可观测不得改变对话行为")
                    .isEqualTo("SUCCESS");
            assertThat(agentTurnTraceMapper.selectBySessionId(opened.getSessionId())).isEmpty();
        } finally {
            properties.getObservability().setEnabled(true);
        }
    }

    // ---------- 查询与清理 ----------

    @Test
    void tracesShouldBeQueryableBySessionInOrder() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("还是有点放不下"));
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("大概是怕重来一次"));

        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(opened.getSessionId());

        assertThat(traces).hasSize(3);
        assertThat(traces).extracting(AgentTurnTrace::getTurnNo).containsExactly(1, 2, 3);
    }

    @Test
    void tracesShouldBeCascadeDeletedWithUser() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));
        assertThat(agentTurnTraceMapper.selectBySessionId(opened.getSessionId())).isNotEmpty();

        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", USER_ID);

        assertThat(agentTurnTraceMapper.selectBySessionId(opened.getSessionId())).isEmpty();
    }

    @Test
    void purgeShouldOnlyRemoveTracesOlderThanRetention() {
        AgentSessionVO opened = openWriting();
        agentChatService.sendMessage(USER_ID, opened.getSessionId(), message(USER_UTTERANCE));

        int deleted = agentTurnTraceMapper.deleteCreatedBefore(LocalDateTime.now().minusDays(90));

        assertThat(deleted).as("刚写入的轨迹不在保留期之外").isZero();
        assertThat(agentTurnTraceMapper.selectBySessionId(opened.getSessionId())).isNotEmpty();
    }

    // ---------- 辅助 ----------

    private AgentTurnTrace onlyTrace(Long sessionId) {
        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(sessionId);
        assertThat(traces).hasSize(1);
        return traces.get(0);
    }

    private String everyTextField(AgentTurnTrace trace) {
        return String.join("|",
                nullSafe(trace.getTraceId()),
                nullSafe(trace.getPurpose()),
                nullSafe(trace.getStage()),
                nullSafe(trace.getStageReason()),
                nullSafe(trace.getModel()),
                nullSafe(trace.getPromptVersion()),
                nullSafe(trace.getPolicyVersion()),
                nullSafe(trace.getOutcome()),
                nullSafe(trace.getCauseType()),
                nullSafe(trace.getDowngradePath()),
                nullSafe(trace.getViolation()),
                nullSafe(trace.getStepsJson()));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 让 provider 变为不可用。
     *
     * 测试 profile 用 mock provider，其可用性由 {@code real-mode-mock-enabled} 决定，
     * 关掉它即等价于「AI 服务未配置」——这是既有 C1 测试制造 UNAVAILABLE 的同一手法，
     * 不需要动任何生产代码。
     *
     * @return 修改前的取值，供 finally 还原
     */
    private boolean setMockEnabled(boolean enabled) {
        boolean previous = appAiProperties.isRealModeMockEnabled();
        appAiProperties.setRealModeMockEnabled(enabled);
        return previous;
    }

    private AgentSessionVO openWriting() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(DRAFT_ID);
        return agentChatService.startOrResume(USER_ID, request);
    }

    private AgentSessionVO openReview() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(UNLOCKED_ID);
        request.setPurpose(AgentSessionPurpose.REVIEW_CHAT);
        return agentChatService.startOrResume(USER_ID, request);
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM agent_turn_trace WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_tool_call WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_message WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_session WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM `record` WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", USER_ID);
    }

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
                "那时在为一件旧物出神", "我以为自己已经忘了",
                LocalDateTime.of(2026, 3, 14, 21, 0), LocalDateTime.of(2026, 3, 14, 21, 0));
    }
}
