package com.flashback.agent.trace;

import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentTurnTrace;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentTurnTraceMapper;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * C5 闸门 3：真实 provider 下的决策轨迹联调探针。
 *
 * <h3>为什么走完整链路而不是只调 model client</h3>
 * C3/C4 的探针只需要观察「模型写出了什么」，直接调 {@code AgentModelClient} 就够。
 * C5 要观察的是**轨迹接线是否完整** —— 采集点有没有漏、早退路径有没有留痕、
 * 耗时是否被记下。这些只有走真实的 {@code AgentChatService.sendMessage} 才能验到，
 * 因为漏采集恰恰发生在编排层。
 *
 * 因此本探针是 {@code @SpringBootTest}：**provider 是真实的，数据库是 H2 测试库**。
 * 真实链路的定义在这里是「真实模型调用 + 真实编排 + 真实落库」，
 * 换掉的只是数据库实例。
 *
 * 观察项与 tasks 的对应：
 * - T-35 真实链路下轨迹完整性（三段是否齐备、早退路径是否留痕）
 * - T-36 {@code provider_duration_ms} 在真实 provider 下的量级
 * - T-37 回看模式下模型是否尝试 tool_calls（fail-closed 是否被活体触发）
 *
 * 安全边界（沿用 C3/C4 探针）：
 * - **默认跳过**，只有 C5_REAL_PROBE=1 时运行，避免混进常规回归产生意外外调；
 * - 只用自造内容，**不使用用户真实日记**；
 * - 只打印结构化指标与判定，**不打印回复全文、不打印轨迹里的任何文本字段之外的内容**；
 * - 写入的是 H2 测试库，不触碰本地 MySQL 的真实数据。
 *
 * 预算：写作引导 3 轮 + 回看 3 轮 = **6 次请求**（上限 10）。
 * 刻意不触发素材生成（不推进到 CLOSING），省下调用余量。
 */
@EnabledIfEnvironmentVariable(named = "C5_REAL_PROBE", matches = "1")
@SpringBootTest
@ActiveProfiles("test")
class C5RealProviderProbeTest {

    private static final Long USER_ID = 9701L;
    private static final Long DRAFT_ID = 5701L;
    private static final Long UNLOCKED_ID = 5702L;

    /** 自造的「被回看记录」，模拟几个月前封存的内容。 */
    private static final String RECORD_CONTENT = "项目排期一直往前压，我每天醒来先想还有多少没做完";
    private static final String RECORD_BELIEF_THEN = "我以为这次真的撑不住了";

    private static final List<String> WRITING_TURNS = List.of(
            "又开始为工作的事睡不着了",
            "主要是怕交不出来，然后被觉得不行",
            "大概我一直很怕让别人失望");

    private static final List<String> REVIEW_TURNS = List.of(
            "现在回头看，好像没有当时想的那么糟",
            "那个项目最后按时交了，只是过程很难受",
            "我大概是太容易把事情想到最坏");

    /**
     * 把真实 provider 配置从环境变量注入，覆盖 application-test.yml 的 mock 设置。
     *
     * 缺省仍是 mock —— 未配置凭证时探针会自行判定并跳过，绝不静默用空 key 发请求。
     */
    @DynamicPropertySource
    static void realProvider(DynamicPropertyRegistry registry) {
        registry.add("app.ai.provider", () -> System.getenv().getOrDefault("AI_PROVIDER", "mock"));
        registry.add("app.ai.base-url",
                () -> System.getenv().getOrDefault("AI_BASE_URL", "https://api.deepseek.com"));
        registry.add("app.ai.api-key", () -> System.getenv().getOrDefault("AI_API_KEY", ""));
        registry.add("app.ai.model", () -> System.getenv().getOrDefault("AI_MODEL", "deepseek-v4-pro"));
        registry.add("app.ai.timeout-millis", () -> "20000");
        registry.add("app.ai.real-mode-mock-enabled", () -> "false");
    }

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AgentTurnTraceMapper agentTurnTraceMapper;

    @Autowired
    private com.flashback.agent.AgentModelClient modelClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void probeRealProviderTraceCompleteness() {
        System.out.printf("C5PROBE provider=%s mockProvider=%s unavailable=%s%n",
                modelClient.provider(),
                modelClient.isMockProvider(),
                String.valueOf(modelClient.unavailableReason()));
        if (modelClient.isMockProvider() || modelClient.unavailableReason() != null) {
            System.out.println("C5PROBE SKIPPED: 未配置真实 provider，未发生任何外调");
            return;
        }

        cleanUp();
        insertUser();
        insertRecord(DRAFT_ID, "DRAFT");
        insertRecord(UNLOCKED_ID, "UNLOCKED");

        // ---------- 写作引导：3 轮 ----------
        AgentSessionStartRequest writingRequest = new AgentSessionStartRequest();
        writingRequest.setRecordId(DRAFT_ID);
        AgentSessionVO writing = agentChatService.startOrResume(USER_ID, writingRequest);
        System.out.printf("C5PROBE writing opened status=%s stage=%s%n",
                writing.getStatus(), writing.getStage());

        for (String turn : WRITING_TURNS) {
            AgentSessionVO result = agentChatService.sendMessage(
                    USER_ID, writing.getSessionId(), message(turn));
            System.out.printf("C5PROBE writing turn status=%s stage=%s pendingTool=%s%n",
                    result.getStatus(), result.getStage(), result.getPendingToolCall() != null);
        }
        reportTraces("writing", writing.getSessionId());

        // ---------- 回看：3 轮 ----------
        AgentSessionStartRequest reviewRequest = new AgentSessionStartRequest();
        reviewRequest.setRecordId(UNLOCKED_ID);
        reviewRequest.setPurpose(AgentSessionPurpose.REVIEW_CHAT);
        AgentSessionVO review = agentChatService.startOrResume(USER_ID, reviewRequest);
        System.out.printf("C5PROBE review opened status=%s stage=%s%n",
                review.getStatus(), review.getStage());

        for (String turn : REVIEW_TURNS) {
            AgentSessionVO result = agentChatService.sendMessage(
                    USER_ID, review.getSessionId(), message(turn));
            System.out.printf("C5PROBE review turn status=%s sessionStatus=%s material=%s%n",
                    result.getStatus(), result.getSessionStatus(), result.getMaterialDraft() != null);
        }
        reportTraces("review", review.getSessionId());

        // ---------- T-36 耗时汇总 ----------
        reportDurations();

        // ---------- 隐私复核：真实回复下轨迹是否仍无原文 ----------
        // 单测已用特征串验过，这里再用真实模型产出复核一次：
        // 真实回复的措辞不可预测，是「实现里某处不小心把文本塞进轨迹」最可能暴露的场景。
        reportPrivacy(writing.getSessionId(), REVIEW_TURNS.get(0));
        reportPrivacy(review.getSessionId(), RECORD_CONTENT);

        System.out.println("C5PROBE DONE");
        cleanUp();
    }

    /**
     * 打印某会话每一轮轨迹的结构化摘要。
     *
     * T-35 的核心：逐轮确认三段是否齐备。缺哪一段在这里会直接显形。
     */
    private void reportTraces(String label, Long sessionId) {
        List<AgentTurnTrace> traces = agentTurnTraceMapper.selectBySessionId(sessionId);
        System.out.printf("C5PROBE %s traceCount=%d%n", label, traces.size());
        for (AgentTurnTrace trace : traces) {
            String steps = trace.getStepsJson() == null ? "" : trace.getStepsJson();
            System.out.printf(
                    "C5PROBE %s trace turn=%d attempt=%d outcome=%s durationMs=%s stage=%s reason=%s "
                            + "downgrade=%s violation=%s model=%s promptV=%s policyV=%s%n",
                    label,
                    trace.getTurnNo(),
                    trace.getAttemptNo(),
                    trace.getOutcome(),
                    String.valueOf(trace.getProviderDurationMs()),
                    trace.getStage(),
                    String.valueOf(trace.getStageReason()),
                    String.valueOf(trace.getDowngradePath()),
                    String.valueOf(trace.getViolation()),
                    trace.getModel(),
                    trace.getPromptVersion(),
                    trace.getPolicyVersion());
            // 三段齐备性：缺任何一项都说明采集点在真实链路上漏了。
            System.out.printf(
                    "C5PROBE %s trace turn=%d segments mode=%s stage=%s memory=%s prompt=%s "
                            + "provider=%s guardrail=%s tools=%s%n",
                    label,
                    trace.getTurnNo(),
                    steps.contains("\"step\":\"mode\""),
                    steps.contains("\"step\":\"stage-decision\"") || steps.contains("\"step\":\"stage-retained\""),
                    steps.contains("\"step\":\"memory-retrieval\""),
                    steps.contains("\"step\":\"prompt\""),
                    steps.contains("\"step\":\"provider\""),
                    steps.contains("\"step\":\"guardrail\""),
                    steps.contains("\"step\":\"tools\""));
            // T-37：fail-closed 是否被活体触发。
            if (steps.contains("tools-fail-closed")) {
                System.out.printf("C5PROBE %s FAIL-CLOSED TRIGGERED turn=%d%n", label, trace.getTurnNo());
            }
            if (steps.contains("reply-clipped")) {
                System.out.printf("C5PROBE %s reply clipped turn=%d%n", label, trace.getTurnNo());
            }
        }
    }

    /**
     * T-36：耗时量级。C5 之前成功路径的耗时被直接丢弃，这项数据在 C5 之后才存在。
     */
    private void reportDurations() {
        List<AgentTurnTrace> all = agentTurnTraceMapper.selectRecentByUserId(USER_ID, 50);
        long count = 0;
        long min = Long.MAX_VALUE;
        long max = 0;
        long sum = 0;
        for (AgentTurnTrace trace : all) {
            Long duration = trace.getProviderDurationMs();
            if (duration == null) {
                continue;
            }
            count++;
            sum += duration;
            min = Math.min(min, duration);
            max = Math.max(max, duration);
        }
        System.out.printf("C5PROBE durations turns=%d minMs=%s avgMs=%s maxMs=%s%n",
                count,
                count == 0 ? "n/a" : String.valueOf(min),
                count == 0 ? "n/a" : String.valueOf(sum / count),
                count == 0 ? "n/a" : String.valueOf(max));
    }

    /**
     * 隐私复核：断言某段文本的任意较长片段都不出现在轨迹的文本列里。
     *
     * 只打印布尔结果，不打印被检查的文本。
     */
    private void reportPrivacy(Long sessionId, String sensitive) {
        String normalized = sensitive.replaceAll("\\s+", "");
        int window = Math.min(8, normalized.length());
        boolean leaked = false;
        List<String> rows = jdbcTemplate.queryForList(
                "SELECT CONCAT_WS('|', purpose, stage, stage_reason, model, prompt_version, "
                        + "policy_version, outcome, cause_type, downgrade_path, violation, steps_json) "
                        + "FROM agent_turn_trace WHERE session_id = ?",
                String.class, sessionId);
        for (String row : rows) {
            String rowNormalized = row == null ? "" : row.replaceAll("\\s+", "");
            for (int i = 0; i + window <= normalized.length(); i++) {
                if (rowNormalized.contains(normalized.substring(i, i + window))) {
                    leaked = true;
                    break;
                }
            }
        }
        System.out.printf("C5PROBE privacy sessionId=%d rows=%d leaked=%s%n",
                sessionId, rows.size(), leaked);
    }

    // ---------- fixture ----------

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

    private void insertUser() {
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, 'c5-probe', 'x', 'c5-probe', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, USER_ID);
    }

    private void insertRecord(Long id, String status) {
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, title, content, status, record_type,
                                      ai_summary, belief_then, created_at, updated_at)
                VALUES (?, ?, '那段日子', ?, ?, 'NODE_RECORD', ?, ?, ?, ?)
                """,
                id, USER_ID, RECORD_CONTENT, status,
                "那时在为项目排期焦虑", RECORD_BELIEF_THEN,
                LocalDateTime.of(2026, 4, 18, 22, 0), LocalDateTime.of(2026, 4, 18, 22, 0));
    }
}
