package com.flashback.agent.reflection;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.AgentWitnessTurnDirective;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentResiliencePolicy;
import com.flashback.agent.temporal.TemporalPatternEvidence;
import com.flashback.agent.temporal.TemporalPolicyResult;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentReplyPipelineTest {

    private AgentPromptBuilder promptBuilder;
    private AgentModelClient modelClient;
    private AgentContentChecker contentChecker;
    private AgentTimeAttributionChecker timeAttributionChecker;
    private AgentGuardrailDowngrade downgrade;
    private AgentReplyPipeline pipeline;

    @BeforeEach
    void setUp() {
        promptBuilder = mock(AgentPromptBuilder.class);
        modelClient = mock(AgentModelClient.class);
        contentChecker = mock(AgentContentChecker.class);
        timeAttributionChecker = mock(AgentTimeAttributionChecker.class);
        downgrade = mock(AgentGuardrailDowngrade.class);
        AgentGuardrailPolicy guardrailPolicy = mock(AgentGuardrailPolicy.class);

        when(modelClient.isMockProvider()).thenReturn(false);
        when(modelClient.model()).thenReturn("scripted");
        when(modelClient.provider()).thenReturn("scripted");
        when(promptBuilder.buildMemorySupplement(any())).thenReturn(null);
        when(promptBuilder.buildConversationMessages(any(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("role", "system", "content", "fixed-system")));
        when(promptBuilder.buildConversationMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("role", "system", "content", "fixed-system")));
        when(promptBuilder.normalizeReplyShape(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(guardrailPolicy.enforceReplyLength(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());
        when(downgrade.safeFallbackReply()).thenReturn("安全兜底");

        pipeline = new AgentReplyPipeline(
                promptBuilder,
                guardrailPolicy,
                modelClient,
                mock(AgentMockResponder.class),
                mock(AgentToolSchemaFactory.class),
                contentChecker,
                downgrade,
                timeAttributionChecker,
                new AgentReflectionPolicy(),
                new AgentResiliencePolicy());
    }

    @Test
    void shouldRewriteEligibleReplyOnceWithoutToolsAndPreserveInitialProposal() throws Exception {
        AgentRawToolCall proposal = new AgentRawToolCall("append_record", "{}");
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("过去内容", List.of(proposal)))
                .thenReturn(new AgentModelResponse("你在过去某个时候写下过这段内容。", List.of()));
        when(timeAttributionChecker.check(any(), any()))
                .thenReturn(AgentGuardrailVerdict.violation(
                        AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION))
                .thenReturn(AgentGuardrailVerdict.pass());
        AgentTraceCollector trace = trace(AgentStage.EMOTION);

        AgentReply reply = generate(AgentStage.EMOTION, true, trace);

        assertThat(reply.success()).isTrue();
        assertThat(reply.content()).isEqualTo("你在过去某个时候写下过这段内容。");
        assertThat(reply.toolCalls()).containsExactly(proposal);
        verify(modelClient, times(2)).completeWithTools(any(), any(), anyBoolean(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, Object>>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Boolean> strictCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(modelClient, times(2)).completeWithTools(
                any(), toolsCaptor.capture(), strictCaptor.capture(), any());
        assertThat(toolsCaptor.getAllValues().get(1)).isEmpty();
        assertThat(strictCaptor.getAllValues().get(1)).isFalse();
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "reflection-result")
                .containsEntry("terminal", "rewritten"));
        assertThat(trace.steps().stream().filter(step -> "provider".equals(step.get("step"))))
                .extracting(step -> step.get("phase"))
                .containsExactly("initial", "reflection");
    }

    @Test
    void shouldNotReflectClosingReply() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("过去内容", List.of()));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.violation(
                AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION));

        AgentReply reply = generate(AgentStage.CLOSING, false, trace(AgentStage.CLOSING));

        assertThat(reply.content()).isEqualTo("安全兜底");
        verify(modelClient, times(1)).completeWithTools(any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldFallbackTemporalOverreachWithoutReflectionCall() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("这证明你以后每次都会这样。", List.of()));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());
        AgentTraceCollector trace = trace(AgentStage.REVIEW);

        AgentReply reply = generate(AgentStage.REVIEW, false, trace);

        assertThat(reply.content()).isEqualTo("安全兜底");
        verify(modelClient, times(1)).completeWithTools(any(), any(), anyBoolean(), any());
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "guardrail")
                .containsEntry("layer", "reply-temporal")
                .containsEntry("violation", "temporal-overreach"));
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "reflection-decision")
                .containsEntry("eligible", false));
    }

    @Test
    void shouldPreserveC8ReplyBehaviorWhenTemporalPolicyIsDisabled() throws Exception {
        String candidate = "这证明你以后每次都会这样。";
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse(candidate, List.of()));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());

        AgentReply reply = generate(AgentStage.REVIEW, false, trace(AgentStage.REVIEW), false);

        assertThat(reply.content()).isEqualTo(candidate);
        verify(modelClient, times(1)).completeWithTools(any(), any(), anyBoolean(), any());
    }

    @Test
    void shouldFallbackAfterFailedRewriteWithoutThirdCall() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("过去内容", List.of()))
                .thenThrow(new IOException("secret-response-must-not-be-traced"));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.violation(
                AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION));
        AgentTraceCollector trace = trace(AgentStage.EMOTION);

        AgentReply reply = generate(AgentStage.EMOTION, false, trace);

        assertThat(reply.content()).isEqualTo("安全兜底");
        assertThat(reply.toolCalls()).isEmpty();
        verify(modelClient, times(2)).completeWithTools(any(), any(), anyBoolean(), any());
        assertThat(trace.steps().toString()).doesNotContain("secret-response-must-not-be-traced");
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "reflection-provider-failed")
                .containsEntry("category", "unknown"));
    }

    @Test
    void shouldFallbackAfterInvalidRewriteWithoutThirdCall() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("过去内容", List.of()))
                .thenReturn(new AgentModelResponse(null, List.of()));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.violation(
                AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION));
        AgentTraceCollector trace = trace(AgentStage.EMOTION);

        AgentReply reply = generate(AgentStage.EMOTION, false, trace);

        assertThat(reply.content()).isEqualTo("安全兜底");
        verify(modelClient, times(2)).completeWithTools(any(), any(), anyBoolean(), any());
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "reflection-result")
                .containsEntry("terminal", "invalid-content"));
    }

    @Test
    void excessiveQuestionsMustReflectOnceThenFallbackWithoutThirdCall() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("为什么这么累？还想再说一点吗？", List.of()))
                .thenReturn(new AgentModelResponse("要先说工作吗？还是先说家里？", List.of()));
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());
        AgentTraceCollector trace = trace(AgentStage.WITNESS);

        AgentReply reply = pipeline.generate(
                AgentStage.WITNESS,
                AgentWitnessTurnDirective.reflectOnly(AgentStage.WITNESS),
                List.of(),
                null,
                "test",
                false,
                null,
                AgentLayeredCorpus.sessionOnly(AgentSourceCorpus.ofTexts(List.of(), 4)),
                List.of(),
                new TemporalPolicyResult(false, List.of(), List.of(),
                        TemporalPatternEvidence.absent(), 0, 0),
                AgentCallBudget.start(20_000),
                trace);

        assertThat(reply.content()).isEqualTo("安全兜底");
        verify(modelClient, times(2)).completeWithTools(any(), any(), anyBoolean(), any());
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "guardrail")
                .containsEntry("layer", "reply-question-limit")
                .containsEntry("violation", "excessive-questions"));
        assertThat(trace.steps()).anySatisfy(step -> assertThat(step)
                .containsEntry("step", "reflection-result")
                .containsEntry("terminal", "fallback"));
    }

    @Test
    void shouldNotReflectInitialProviderFailure() throws Exception {
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenThrow(new IOException("provider down"));

        AgentReply reply = generate(AgentStage.EMOTION, false, trace(AgentStage.EMOTION));

        assertThat(reply.success()).isFalse();
        verify(modelClient, times(1)).completeWithTools(any(), any(), anyBoolean(), any());
        verify(timeAttributionChecker, never()).check(any(), any());
    }

    @Test
    void shouldRestoreInterruptedFlagAndNeverRetryInterruptedCall() throws Exception {
        Thread.interrupted();
        when(modelClient.completeWithTools(any(), any(), anyBoolean(), any()))
                .thenThrow(new InterruptedException("sensitive"));
        AgentTraceCollector trace = trace(AgentStage.EMOTION);
        try {
            AgentReply reply = generate(AgentStage.EMOTION, false, trace);

            assertThat(reply.success()).isFalse();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(modelClient, times(1)).completeWithTools(any(), any(), anyBoolean(), any());
            assertThat(trace.causeType()).isEqualTo("interrupted");
        } finally {
            Thread.interrupted();
        }
    }

    private AgentReply generate(AgentStage stage, boolean toolsEnabled, AgentTraceCollector trace) {
        return generate(stage, toolsEnabled, trace, true);
    }

    private AgentReply generate(
            AgentStage stage, boolean toolsEnabled, AgentTraceCollector trace, boolean temporalEnabled) {
        return pipeline.generate(
                stage,
                List.of(),
                null,
                "test",
                toolsEnabled,
                null,
                AgentLayeredCorpus.sessionOnly(null),
                List.of(),
                new TemporalPolicyResult(temporalEnabled, List.of(), List.of(),
                        TemporalPatternEvidence.absent(), 0, 0),
                AgentCallBudget.start(24_000),
                trace);
    }

    private static AgentTraceCollector trace(AgentStage stage) {
        return new AgentTraceCollector(
                1L, 2L, 3L, 1, 1, AgentSessionPurpose.WRITING_GUIDANCE, stage);
    }
}
