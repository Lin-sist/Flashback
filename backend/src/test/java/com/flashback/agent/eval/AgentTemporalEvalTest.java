package com.flashback.agent.eval;

import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.domain.AgentSessionPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.flashback.agent.eval.AgentEvalHarnessTest.stepValue;
import static org.assertj.core.api.Assertions.assertThat;

/** C9 固定时钟回归：复用 C6 scripted provider 与真实生产编排。 */
@DisplayName("C9 时间智能离线评测")
class AgentTemporalEvalTest {

    @Test
    void shouldExposeOnlyTemporalMetadataAndUseNoExtraProviderCall() {
        String marker = "紫罗兰色的旧铁皮盒子";
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .purpose(AgentSessionPurpose.REVIEW_CHAT)
                .recordContent("当时写下了关于方向的犹豫")
                .memoryCandidate(70001L, marker + "，以前也有过类似的方向犹豫",
                        LocalDateTime.of(2026, 1, 1, 10, 0))
                .memoryCandidate(70002L, "以前也有过类似的方向犹豫",
                        LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply("你觉得它们像吗？"));

        harness.turn("以前也有过类似的时候吗？");

        AgentTraceCollector trace = harness.sink().last();
        java.util.Map<String, Object> temporalStep = trace.steps().stream()
                .filter(step -> "temporal".equals(step.get("step")))
                .findFirst().orElseThrow();
        assertThat(temporalStep.keySet()).containsExactlyInAnyOrder(
                "step", "enabled", "recentCount", "distantCount", "longAgoCount", "unknownCount",
                "beforeChars", "afterChars", "recurrenceEligible");
        assertThat(stepValue(trace, "temporal", "enabled")).isEqualTo(true);
        assertThat(stepValue(trace, "temporal", "recurrenceEligible")).isEqualTo(true);
        assertThat((Integer) stepValue(trace, "temporal", "afterChars"))
                .isLessThanOrEqualTo((Integer) stepValue(trace, "temporal", "beforeChars"));
        assertThat(harness.client().replyCallCount()).isEqualTo(1);
        assertThat(trace.steps().toString()).doesNotContain(marker);
    }

    @Test
    void temporalOverreachMustFallbackWithoutReflectionOrSecondCall() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .purpose(AgentSessionPurpose.REVIEW_CHAT)
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply(
                "这证明你以后每次都会这样，已经形成趋势。"));

        harness.turn("现在回头看看那时候");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(trace.downgradePath()).isEqualTo("reply-temporal");
        assertThat(trace.violation()).isEqualTo("temporal-overreach");
        assertThat(harness.client().replyCallCount()).isEqualTo(1);
        assertThat(stepValue(trace, "reflection-decision", "eligible")).isEqualTo(false);
        assertThat(harness.messages()).extracting(message -> message.getContent())
                .doesNotContain("这证明你以后每次都会这样，已经形成趋势。");
    }
}
