package com.flashback.agent.trace;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.AgentStageDecision;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 轨迹收集器单测（C5）。
 *
 * 本类的重点不是「字段有没有存进去」，而是**隐私在类型层是否被钉死**：
 * 收集器的公开方法不接受任意文本内容，因此把日记原文传进轨迹在编译期就做不到。
 * 这比运行时校验可靠——运行时校验总有绕过的路径，类型层没有。
 */
class AgentTraceCollectorTest {

    private AgentTraceCollector newCollector() {
        return new AgentTraceCollector(
                7L, 3L, 11L, 2, 1, AgentSessionPurpose.WRITING_GUIDANCE, AgentStage.EMOTION);
    }

    // ---------- 隐私的结构性保证 ----------

    /**
     * 公开收集方法允许的 String 形参**白名单**。
     *
     * 存在理由：这条断言是「不接受任意文本」这个设计约束的可执行版本。
     * 若将来有人给收集器加一个 {@code replyText(String)} 之类的方法，
     * 本测试会失败并逼他解释——而不是等到某次排查时发现轨迹里躺着用户的日记。
     *
     * 白名单里的三个都不是用户表达：model 是配置值、causeType 是异常类名、
     * reasonId / version 是结构化常量短标识。
     */
    private static final List<String> ALLOWED_STRING_PARAM_METHODS = List.of(
            "provider", // model：配置值
            "providerFailed", // operation：内部操作标识
            "providerInvalidContent",
            "toolRejected", // reasonId：AgentToolValidationResult 的结构化常量
            "materialFailed", // causeType：异常类名
            "sessionEnded", // reasonId：内部常量
            "versions"); // 内容哈希派生的版本号

    @Test
    void collectorMustNotAcceptArbitraryTextParameters() {
        for (Method method : AgentTraceCollector.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) {
                continue;
            }
            boolean hasStringParam = false;
            for (Class<?> type : method.getParameterTypes()) {
                if (type == String.class) {
                    hasStringParam = true;
                    break;
                }
            }
            if (!hasStringParam) {
                continue;
            }
            assertThat(ALLOWED_STRING_PARAM_METHODS)
                    .as("收集方法 %s 接受 String 形参。若它承载的不是配置值 / 异常类名 / "
                            + "结构化常量，就不该存在——轨迹禁止承载用户表达。"
                            + "确实合规请显式加入白名单并说明。", method.getName())
                    .contains(method.getName());
        }
    }

    // ---------- 采集内容 ----------

    @Test
    void shouldGenerateOpaqueTraceId() {
        AgentTraceCollector collector = newCollector();

        assertThat(collector.traceId()).hasSize(32).doesNotContain("-");
    }

    @Test
    void shouldCollectStageDecisionAndAdvanceStage() {
        AgentTraceCollector collector = newCollector();

        collector.stageDecision(AgentStage.EMOTION, AgentStage.CONFUSION, AgentStageDecision.Reason.ADVANCE);

        assertThat(collector.stage()).isEqualTo(AgentStage.CONFUSION);
        assertThat(collector.stageReason()).isEqualTo(AgentStageDecision.Reason.ADVANCE);
        assertThat(firstStepOfType(collector, "stage-decision"))
                .containsEntry("from", "EMOTION")
                .containsEntry("to", "CONFUSION")
                .containsEntry("reason", "ADVANCE");
    }

    @Test
    void stageRetainedShouldNotFabricateAReason() {
        AgentTraceCollector collector = newCollector();

        collector.stageRetained(AgentStage.REVIEW);

        assertThat(collector.stageReason())
                .as("回看无阶段机；伪造一个判定结论会让轨迹说谎")
                .isNull();
    }

    @Test
    void shouldDistinguishMemoryDisabledFailedAndEmpty() {
        Map<String, Object> disabled = firstStepOfType(
                newCollector().memoryRetrieval(false, false, false, 0, 0, 0), "memory-retrieval");
        Map<String, Object> failed = firstStepOfType(
                newCollector().memoryRetrieval(true, true, false, 2, 1, 0), "memory-retrieval");
        Map<String, Object> empty = firstStepOfType(
                newCollector().memoryRetrieval(true, false, true, 2, 1, 0), "memory-retrieval");

        assertThat(disabled).containsEntry("enabled", false);
        assertThat(failed).containsEntry("enabled", true).containsEntry("failed", true);
        assertThat(empty)
                .as("检索成功但无命中，与开关关闭、检索失败必须是三种不同的记录")
                .containsEntry("enabled", true)
                .containsEntry("failed", false)
                .containsEntry("retrievedCount", 0);
    }

    @Test
    void shouldRecordProviderDurationOnSuccess() {
        AgentTraceCollector collector = newCollector();

        collector.provider("deepseek-v4-pro", 1234L, false, true);

        assertThat(collector.providerDurationMs()).isEqualTo(1234L);
        assertThat(collector.model()).isEqualTo("deepseek-v4-pro");
        assertThat(collector.outcome())
                .as("成功调用不改变 outcome")
                .isEqualTo(AgentTraceOutcome.SUCCESS);
    }

    @Test
    void providerFailureShouldRecordExceptionTypeOnly() {
        AgentTraceCollector collector = newCollector();

        collector.providerFailed("turn", java.io.IOException.class);

        assertThat(collector.outcome()).isEqualTo(AgentTraceOutcome.FAILED);
        assertThat(collector.causeType()).isEqualTo("IOException");
    }

    @Test
    void downgradeShouldMarkOutcomeAndCarryLocalFallbackFlag() {
        AgentTraceCollector collector = newCollector();

        collector.downgrade(
                AgentTraceLayer.REPLY_ATTRIBUTION,
                AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION,
                true);

        assertThat(collector.outcome())
                .as("对用户是一次成功返回，但排查时必须看出这句话不是 provider 的正常产出")
                .isEqualTo(AgentTraceOutcome.DOWNGRADED);
        assertThat(collector.downgradePath()).isEqualTo("reply-attribution");
        assertThat(collector.violation()).isEqualTo("missing-time-attribution");
        assertThat(firstStepOfType(collector, "downgrade")).containsEntry("fallback", "local");
    }

    @Test
    void downgradeMustNotOverrideAnEarlierFailure() {
        AgentTraceCollector collector = newCollector();

        collector.providerFailed("turn", RuntimeException.class);
        collector.downgrade(AgentTraceLayer.MATERIAL_CONTENT, AgentGuardrailViolation.DIAGNOSTIC, false);

        assertThat(collector.outcome())
                .as("已经失败的一轮不该因为随后的降级被记成 DOWNGRADED")
                .isEqualTo(AgentTraceOutcome.FAILED);
    }

    @Test
    void guardrailStepShouldCarryMetricsNotText() {
        AgentTraceCollector collector = newCollector();

        collector.guardrail(
                AgentTraceLayer.REPLY_CONTENT,
                AgentGuardrailVerdict.violation(AgentGuardrailViolation.DIAGNOSTIC, 0.42d, 9, 30));

        assertThat(firstStepOfType(collector, "guardrail"))
                .containsEntry("layer", "reply-content")
                .containsEntry("passed", false)
                .containsEntry("violation", "diagnostic")
                .containsEntry("coverage", 0.42d)
                .containsEntry("maxUncoveredRun", 9)
                .containsEntry("checkedLength", 30);
    }

    @Test
    void materialFailureMustNotChangeTurnOutcome() {
        AgentTraceCollector collector = newCollector();

        collector.materialFailed("IOException");

        assertThat(collector.outcome())
                .as("素材是可选产物，缺失时对话本身仍是成功的")
                .isEqualTo(AgentTraceOutcome.SUCCESS);
    }

    @Test
    void shouldCollectModeAndToolSteps() {
        AgentTraceCollector collector = newCollector();

        collector.mode(AgentChatMode.of(AgentSessionPurpose.REVIEW_CHAT), 6);
        collector.tools(false, 1, 0);
        collector.toolsFailClosed(1);

        assertThat(firstStepOfType(collector, "mode"))
                .containsEntry("stageMachineDriven", false)
                .containsEntry("toolsAvailable", false)
                .containsEntry("maxTurns", 6);
        assertThat(firstStepOfType(collector, "tools-fail-closed")).containsEntry("discardedCount", 1);
    }

    @Test
    void nullSafeCollectionShouldNotAddSteps() {
        AgentTraceCollector collector = newCollector();

        collector.guardrail(null, null);
        collector.guardrail(AgentTraceLayer.REPLY_CONTENT, null);

        assertThat(collector.steps()).isEmpty();
    }

    // ---------- 辅助 ----------

    private Map<String, Object> firstStepOfType(AgentTraceCollector collector, String type) {
        return collector.steps().stream()
                .filter(step -> type.equals(step.get("step")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到步骤: " + type));
    }
}
