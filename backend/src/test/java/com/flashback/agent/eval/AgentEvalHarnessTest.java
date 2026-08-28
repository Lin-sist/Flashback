package com.flashback.agent.eval;

import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 评测基座自检（C6，T-03 / T-05）。
 *
 * <h3>为什么基座自己需要测</h3>
 * 后面所有维度断言都建立在「基座真的驱动了生产链路」这个前提上。
 * 若基座其实没走到真实分支（例如仍走 mock provider），那么八个维度会全绿，
 * 而绿的是我的替身而不是 Agent——这正是本刀最想避免的自欺形态。
 *
 * 本类因此专门证明四件 {@code AgentMockResponder} <b>做不到</b>的事
 * （proposal E6 / E7、验收 6）：产出违规、组装上下文、被拒的工具提议、provider 失败。
 */
@DisplayName("C6 评测基座自检")
class AgentEvalHarnessTest {

    /**
     * 基座走的是非 mock 分支——这是全部后续维度的前提。
     *
     * 判据取 {@code prompt} 步骤是否存在：mock 分支在 {@code isMockProvider()}
     * 处直接 return，压根不组装 prompt，所以这一条同时证明了
     * 「上下文组装维度可评」与「没有悄悄走回 mock」。
     */
    @Test
    void harnessMustDriveTheRealProviderBranchSoContextAssemblyIsObservable() {
        AgentEvalHarness harness = AgentEvalHarness.builder().build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply("这种感觉是从什么时候开始的？"));

        harness.turn("最近工作上有点撑不住，想写下来");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(trace).isNotNull();
        assertThat(stepTypes(trace))
                .as("mock 分支不组装 prompt；有 prompt 步骤即证明走的是真实 provider 分支")
                .contains("prompt");
        assertThat(trace.model()).isEqualTo("deepseek-v4-pro");
        assertThat(harness.client().replyCallCount()).isEqualTo(1);
        // provider 边界实际收到的消息条数应与轨迹采集到的一致——
        // 对不上说明采集点与真实调用脱节。
        assertThat(harness.client().promptMessageCounts()).hasSize(1);
        assertThat(stepValue(trace, "prompt", "messageCount"))
                .isEqualTo(harness.client().promptMessageCounts().get(0));
    }

    /**
     * 路径一：回复被护栏降级。
     *
     * 用真实的 {@code AgentContentChecker} 与真实规则词表判定——
     * 「你这是」落在 Agent 新增区段即命中 DIAGNOSTIC。
     */
    @Test
    void harnessMustBeAbleToProduceADowngradedReply() {
        AgentEvalHarness harness = AgentEvalHarness.builder().build();
        harness.client().scriptReply(
                ScriptedAgentModelClient.Scripted.reply("你这是焦虑症的典型症状，建议你就医。"));

        harness.turn("最近老是心慌，睡不着，什么都不想做");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(trace.outcome().name())
                .as("AgentMockResponder 的六句文案永远不会被降级，这条路只有 scripted 替身能走到")
                .isEqualTo("DOWNGRADED");
        assertThat(trace.downgradePath()).isEqualTo("reply-content");
        assertThat(trace.violation()).isEqualTo("diagnostic");
    }

    /**
     * 路径二：工具提议被护栏拒绝。
     *
     * 走真实 {@code AgentToolValidator}：正文参数只认会话层，
     * 而这段文字用户从未说过，故判 unfaithful-args。
     */
    @Test
    void harnessMustBeAbleToProduceARejectedToolProposal() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .stage(AgentStage.CORE_QUESTION)
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.replyWithTool(
                "要不要把这段放进正文？",
                new AgentRawToolCall(
                        "append_record_content",
                        "{\"text\":\"我最近心里空得厉害，完全不知道未来该往哪里走，整个人都悬着\","
                                + "\"askText\":\"要不要把这段放进正文？\"}")));

        harness.turn("其实我只是有点累");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(stepTypes(trace)).contains("tool-rejected");
        assertThat(stepValue(trace, "tool-rejected", "reason")).isEqualTo("unfaithful-args");
        assertThat(stepValue(trace, "tools", "proposedCount")).isEqualTo(0);
    }

    /**
     * 路径三：provider 失败。
     *
     * 既有语义必须不变：用户消息已落库、Agent 回复未落库、本轮可重试。
     */
    @Test
    void harnessMustBeAbleToProduceAProviderFailure() {
        AgentEvalHarness harness = AgentEvalHarness.builder().build();
        harness.client().scriptReply(
                ScriptedAgentModelClient.Scripted.failure(AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE));

        harness.turn("今天心里有点乱");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(trace.outcome().name()).isEqualTo("FAILED");
        assertThat(trace.causeType())
                .as("只记稳定分类，不记异常消息——消息可能回带请求内容")
                .isEqualTo("upstream-unavailable");
        assertThat(trace.providerDurationMs())
                .as("失败路径同样记耗时")
                .isNotNull();
    }

    /**
     * 路径四：回看模式下工具提议被 fail-closed 丢弃。
     *
     * 这条分支在 C3b 与 C5 的真实观察里都未活体触发（R10），
     * 之前只有单测覆盖。基座能稳定驱动它。
     */
    @Test
    void harnessMustBeAbleToProduceToolFailClosedInReviewMode() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .purpose(AgentSessionPurpose.REVIEW_CHAT)
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.replyWithTool(
                "那时候的你大概也没想到会走到今天吧。",
                new AgentRawToolCall("append_record_content", "{\"text\":\"随便\",\"askText\":\"要不要？\"}")));

        harness.turn("现在回头看，那阵子其实挺难的");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(stepTypes(trace)).contains("tools-fail-closed");
        assertThat(stepValue(trace, "tools-fail-closed", "discardedCount")).isEqualTo(1);
        assertThat(trace.stage()).isEqualTo(AgentStage.REVIEW);
        assertThat(trace.stageReason())
                .as("回看不经阶段机，不得伪造一个判定结论")
                .isNull();
    }

    /**
     * 记忆注入走真实 {@code MySqlMemoryPort}：候选记录经它取材、截断、按 limit 收口。
     *
     * 断言的是生产逻辑而不是替身——若这里用假 port 返回写死的片段，
     * 「注入预算」维度就变成断言我自己填的数字。
     */
    @Test
    void harnessMustDriveTheRealMemoryPort() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .crossRecordMemoryEnabled(true)
                .memoryCandidate(70001L, "那时候在纠结要不要换个方向", LocalDateTime.of(2026, 3, 14, 21, 0))
                .memoryCandidate(70002L, "写下这些的时候心里挺沉的", LocalDateTime.of(2026, 4, 2, 20, 0))
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply("这种感觉是从什么时候开始的？"));

        harness.turn("最近又开始纠结方向的事情了");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(stepValue(trace, "memory-retrieval", "enabled")).isEqualTo(true);
        assertThat(stepValue(trace, "memory-retrieval", "failed")).isEqualTo(false);
        assertThat(stepValue(trace, "memory-injected", "injectedCount")).isEqualTo(2);
        assertThat(stepValue(trace, "prompt", "memorySupplement")).isEqualTo(true);
    }

    /**
     * 多轮驱动：历史必须真的增长，否则阶段推进与追问上限都走不到。
     */
    @Test
    void harnessMustGrowHistoryAcrossTurns() {
        AgentEvalHarness harness = AgentEvalHarness.builder().build();
        harness.client()
                .scriptReply(ScriptedAgentModelClient.Scripted.reply("这种感觉是从什么时候开始的？"))
                .scriptReply(ScriptedAgentModelClient.Scripted.reply("让你卡住的是具体某件事吗？"));

        harness.turn("最近工作上有点撑不住").turn("大概是从上个月开始的吧");

        assertThat(harness.sink().traces()).hasSize(2);
        assertThat(harness.sink().traces().get(0).turnNo()).isEqualTo(1);
        assertThat(harness.sink().traces().get(1).turnNo()).isEqualTo(2);
        // 开场 1 条 + 两轮各 user/assistant 2 条。
        assertThat(harness.messages()).hasSize(5);
    }

    @Test
    void reflectionCallsMustObserveTheSameRequestScopedBudget() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .crossRecordMemoryEnabled(true)
                .memoryCandidate(70001L, "那阵子一直在纠结要不要换个方向，怕选错了就回不去了",
                        LocalDateTime.of(2026, 3, 14, 21, 0))
                .build();
        harness.client()
                .scriptReply(ScriptedAgentModelClient.Scripted.reply(
                        "你一直在纠结要不要换个方向，怕选错了就回不去了。"))
                .scriptReply(ScriptedAgentModelClient.Scripted.reply(
                        "我记得你过去某个时候也在纠结要不要换个方向，怕选错了就回不去了。"));

        harness.turn("又开始纠结方向的事情了");

        assertThat(harness.client().observedBudgets()).hasSize(2);
        assertThat(harness.client().observedBudgets().get(0))
                .isSameAs(harness.client().observedBudgets().get(1));
    }

    // ---------- 读取轨迹步骤的小工具 ----------

    static java.util.List<String> stepTypes(AgentTraceCollector trace) {
        return trace.steps().stream().map(step -> String.valueOf(step.get("step"))).toList();
    }

    static Object stepValue(AgentTraceCollector trace, String stepType, String key) {
        for (java.util.Map<String, Object> step : trace.steps()) {
            if (stepType.equals(step.get("step")) && step.containsKey(key)) {
                return step.get(key);
            }
        }
        return null;
    }
}
