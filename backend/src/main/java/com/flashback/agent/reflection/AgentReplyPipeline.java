package com.flashback.agent.reflection;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.agent.resilience.AgentProviderFailures;
import com.flashback.agent.resilience.AgentResiliencePolicy;
import com.flashback.agent.temporal.AgentTemporalLanguageChecker;
import com.flashback.agent.temporal.TemporalPatternEvidence;
import com.flashback.agent.temporal.TemporalPolicyResult;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.agent.trace.AgentTraceLayer;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * C7：回复生成、确定性检查与一次受控 reflection。
 *
 * Reflection 只修复非 CLOSING 的时间归属违规；不处理 provider 错误、不下发 tools、
 * 不接收候选片段作为指令输入。Material 路径不属于本组件。
 */
public final class AgentReplyPipeline {

    private static final Logger log = LoggerFactory.getLogger(AgentReplyPipeline.class);

    private final AgentPromptBuilder promptBuilder;
    private final AgentGuardrailPolicy guardrailPolicy;
    private final AgentModelClient modelClient;
    private final AgentMockResponder mockResponder;
    private final AgentToolSchemaFactory toolSchemaFactory;
    private final AgentContentChecker contentChecker;
    private final AgentGuardrailDowngrade guardrailDowngrade;
    private final AgentTimeAttributionChecker timeAttributionChecker;
    private final AgentReflectionPolicy reflectionPolicy;
    private final AgentResiliencePolicy resiliencePolicy;
    private final AgentTemporalLanguageChecker temporalLanguageChecker = new AgentTemporalLanguageChecker();

    public AgentReplyPipeline(
            AgentPromptBuilder promptBuilder,
            AgentGuardrailPolicy guardrailPolicy,
            AgentModelClient modelClient,
            AgentMockResponder mockResponder,
            AgentToolSchemaFactory toolSchemaFactory,
            AgentContentChecker contentChecker,
            AgentGuardrailDowngrade guardrailDowngrade,
            AgentTimeAttributionChecker timeAttributionChecker,
            AgentReflectionPolicy reflectionPolicy,
            AgentResiliencePolicy resiliencePolicy) {
        this.promptBuilder = promptBuilder;
        this.guardrailPolicy = guardrailPolicy;
        this.modelClient = modelClient;
        this.mockResponder = mockResponder;
        this.toolSchemaFactory = toolSchemaFactory;
        this.contentChecker = contentChecker;
        this.guardrailDowngrade = guardrailDowngrade;
        this.timeAttributionChecker = timeAttributionChecker;
        this.reflectionPolicy = reflectionPolicy;
        this.resiliencePolicy = resiliencePolicy;
    }

    public AgentReply generate(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt,
            String operation,
            boolean toolsEnabled,
            String toolSupplement,
            AgentLayeredCorpus corpus,
            List<MemoryFragment> injectedMemory,
            AgentCallBudget budget,
            AgentTraceCollector trace) {
        return generate(targetStage, history, draftExcerpt, operation, toolsEnabled, toolSupplement,
                corpus, injectedMemory,
                new TemporalPolicyResult(false, injectedMemory, List.of(),
                        TemporalPatternEvidence.absent(), 0, 0),
                budget, trace);
    }

    public AgentReply generate(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt,
            String operation,
            boolean toolsEnabled,
            String toolSupplement,
            AgentLayeredCorpus corpus,
            List<MemoryFragment> injectedMemory,
            TemporalPolicyResult temporal,
            AgentCallBudget budget,
            AgentTraceCollector trace) {
        if (modelClient.isMockProvider()) {
            return generateMock(targetStage, history, operation, toolsEnabled, corpus,
                    temporal != null && temporal.enabled(), recurrenceEligible(temporal), trace);
        }

        List<Map<String, Object>> tools = toolsEnabled
                ? toolSchemaFactory.buildTools(modelClient.useStrictMode())
                : List.of();
        String memorySupplement = promptBuilder.buildMemorySupplement(injectedMemory);
        String temporalSupplement = promptBuilder.buildTemporalSupplement(temporal);
        String contextualSupplement = joinSupplements(memorySupplement, temporalSupplement);
        List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                targetStage, history, draftExcerpt, toolSupplement, contextualSupplement);
        tracePrompt(trace, messages.size(), toolSupplement, contextualSupplement, draftExcerpt);

        long startedAt = System.nanoTime();
        try {
            AgentModelResponse response = modelClient.completeWithTools(
                    messages, tools, toolsEnabled && modelClient.useStrictMode(), budget);
            long durationMs = millisSince(startedAt);

            String candidate = replyOf(response, true);
            if (candidate == null) {
                logProviderIssue(
                        operation, targetStage, startedAt, AgentProviderFailureCategory.INVALID_RESPONSE);
                traceOf(trace, t -> t.provider(
                        AgentProviderPhase.INITIAL, modelClient.model(), durationMs, false, false)
                        .providerFailed(
                                AgentProviderPhase.INITIAL,
                                operation,
                                AgentProviderFailureCategory.INVALID_RESPONSE,
                                budget));
                return AgentReply.fail(resiliencePolicy.failureMessage(
                        operation, targetStage, AgentProviderFailureCategory.INVALID_RESPONSE));
            }
            traceOf(trace, t -> t.provider(
                    AgentProviderPhase.INITIAL, modelClient.model(), durationMs, false, true));

            List<AgentRawToolCall> initialToolCalls = response.toolCalls();
            GuardedReply guarded = checkReply(
                    promptBuilder.normalizeReplyShape(candidate), corpus,
                    temporal != null && temporal.enabled(), recurrenceEligible(temporal), trace);
            if (guarded.passed()) {
                return AgentReply.ok(enforceLength(guarded.value(), trace), initialToolCalls);
            }

            Optional<String> instruction = reflectionPolicy.instructionFor(targetStage, guarded.violation());
            traceOf(trace, t -> t.reflectionDecision(
                    instruction.isPresent(), guarded.violation(), AgentReflectionPolicy.MAX_REFLECTION_REWRITES));
            if (instruction.isEmpty()) {
                traceOf(trace, t -> t.reflectionResult(
                        false, false, AgentReflectionTerminal.FALLBACK));
                return fallback(guarded, operation, trace);
            }

            return reflect(
                    targetStage, operation, messages, instruction.get(), corpus,
                    temporal != null && temporal.enabled(), recurrenceEligible(temporal),
                    initialToolCalls, guarded, budget, trace);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            AgentProviderFailureCategory category = AgentProviderFailures.fromThrowable(ex);
            logProviderIssue(operation, targetStage, startedAt, category);
            long durationMs = millisSince(startedAt);
            traceOf(trace, t -> t.provider(
                    AgentProviderPhase.INITIAL, modelClient.model(), durationMs, false, false)
                    .providerFailed(AgentProviderPhase.INITIAL, operation, category, budget));
            return AgentReply.fail(resiliencePolicy.failureMessage(operation, targetStage, category));
        }
    }

    private AgentReply reflect(
            AgentStage targetStage,
            String operation,
            List<Map<String, String>> initialMessages,
            String instruction,
            AgentLayeredCorpus corpus,
            boolean temporalEnabled,
            boolean recurrenceEligible,
            List<AgentRawToolCall> initialToolCalls,
            GuardedReply initialGuarded,
            AgentCallBudget budget,
            AgentTraceCollector trace) {
        List<Map<String, String>> reflectionMessages = new ArrayList<>(initialMessages);
        reflectionMessages.add(Map.of("role", "user", "content", instruction));
        long startedAt = System.nanoTime();
        try {
            AgentModelResponse response = modelClient.completeWithTools(
                    List.copyOf(reflectionMessages), List.of(), false, budget);
            long durationMs = millisSince(startedAt);
            if (response.content() == null) {
                traceOf(trace, t -> t.provider(
                        AgentProviderPhase.REFLECTION, modelClient.model(), durationMs, false, false)
                        .reflectionProviderFailed(AgentProviderFailureCategory.INVALID_RESPONSE, budget)
                        .reflectionResult(
                        true, false, AgentReflectionTerminal.INVALID_CONTENT));
                return fallback(initialGuarded, operation, trace);
            }
            traceOf(trace, t -> t.provider(
                    AgentProviderPhase.REFLECTION, modelClient.model(), durationMs, false, true));

            GuardedReply rewritten = checkReply(
                    promptBuilder.normalizeReplyShape(response.content()), corpus,
                    temporalEnabled, recurrenceEligible, trace);
            if (!rewritten.passed()) {
                traceOf(trace, t -> t.reflectionResult(
                        true, false, AgentReflectionTerminal.FALLBACK));
                return fallback(rewritten, operation, trace);
            }
            traceOf(trace, t -> t.reflectionResult(
                    true, true, AgentReflectionTerminal.REWRITTEN));
            return AgentReply.ok(enforceLength(rewritten.value(), trace), initialToolCalls);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            AgentProviderFailureCategory category = AgentProviderFailures.fromThrowable(ex);
            logProviderIssue(operation + "-reflection", targetStage, startedAt, category);
            long durationMs = millisSince(startedAt);
            traceOf(trace, t -> t.provider(
                    AgentProviderPhase.REFLECTION, modelClient.model(), durationMs, false, false)
                    .reflectionProviderFailed(category, budget)
                    .reflectionResult(true, false, AgentReflectionTerminal.PROVIDER_FAILED));
            return fallback(initialGuarded, operation, trace);
        }
    }

    private AgentReply generateMock(
            AgentStage targetStage,
            List<AgentMessage> history,
            String operation,
            boolean toolsEnabled,
            AgentLayeredCorpus corpus,
            boolean temporalEnabled,
            boolean recurrenceEligible,
            AgentTraceCollector trace) {
        String latestUserInput = latestUserContent(history);
        long startedAt = System.nanoTime();
        GuardedReply guarded = checkReply(
                mockResponder.reply(targetStage, latestUserInput), corpus,
                temporalEnabled, recurrenceEligible, trace);
        List<AgentRawToolCall> toolCalls = mockResponder.toolCalls(targetStage, latestUserInput, toolsEnabled);
        long durationMs = millisSince(startedAt);
        traceOf(trace, t -> t.provider(
                AgentProviderPhase.INITIAL, modelClient.provider(), durationMs, true, true));
        if (!guarded.passed()) {
            return fallback(guarded, operation, trace);
        }
        return AgentReply.ok(enforceLength(guarded.value(), trace), toolCalls);
    }

    private GuardedReply checkReply(
            String reply,
            AgentLayeredCorpus corpus,
            boolean temporalEnabled,
            boolean recurrenceEligible,
            AgentTraceCollector trace) {
        if (reply == null) {
            return GuardedReply.passed(null);
        }
        AgentGuardrailVerdict content = contentChecker.check(reply, corpus.combined());
        traceOf(trace, t -> t.guardrail(AgentTraceLayer.REPLY_CONTENT, content));
        if (!content.isPassed()) {
            return GuardedReply.failed(reply, AgentTraceLayer.REPLY_CONTENT, content);
        }
        AgentGuardrailVerdict attribution = timeAttributionChecker.check(reply, corpus);
        traceOf(trace, t -> t.guardrail(AgentTraceLayer.REPLY_ATTRIBUTION, attribution));
        if (!attribution.isPassed()) {
            return GuardedReply.failed(reply, AgentTraceLayer.REPLY_ATTRIBUTION, attribution);
        }
        if (temporalEnabled) {
            AgentGuardrailVerdict temporal = temporalLanguageChecker.check(reply, recurrenceEligible);
            traceOf(trace, t -> t.guardrail(AgentTraceLayer.REPLY_TEMPORAL, temporal));
            if (!temporal.isPassed()) {
                return GuardedReply.failed(reply, AgentTraceLayer.REPLY_TEMPORAL, temporal);
            }
        }
        return GuardedReply.passed(reply);
    }

    private boolean recurrenceEligible(TemporalPolicyResult temporal) {
        return temporal != null && temporal.enabled()
                && temporal.patternEvidence() != null
                && temporal.patternEvidence().eligible();
    }

    private String joinSupplements(String first, String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "\n\n" + second;
    }

    private AgentReply fallback(
            GuardedReply guarded, String operation, AgentTraceCollector trace) {
        AgentGuardrailVerdict verdict = guarded.verdict();
        String path = guarded.layer() == AgentTraceLayer.REPLY_ATTRIBUTION
                ? "reply-attribution:" + operation
                : guarded.layer() == AgentTraceLayer.REPLY_TEMPORAL
                ? "reply-temporal:" + operation
                : "reply:" + operation;
        guardrailDowngrade.trace(path, traceSessionId(trace), traceTurnNo(trace), verdict);
        traceOf(trace, t -> t.downgrade(guarded.layer(), verdict.violation(), true));
        return AgentReply.ok(guardrailDowngrade.safeFallbackReply(), List.of());
    }

    private String enforceLength(String reply, AgentTraceCollector trace) {
        if (reply == null) {
            return null;
        }
        int beforeLength = reply.trim().length();
        String limited = guardrailPolicy.enforceReplyLength(reply);
        int afterLength = limited == null ? 0 : limited.length();
        if (afterLength < beforeLength) {
            traceOf(trace, t -> t.replyClipped(beforeLength, afterLength));
        }
        return limited;
    }

    private String replyOf(AgentModelResponse response, boolean allowToolAskText) {
        String reply = response.content();
        if (reply == null && allowToolAskText && response.hasToolCalls()) {
            reply = modelClient.readArgumentText(
                    response.firstToolCall().arguments(), AgentToolRegistry.PARAM_ASK_TEXT);
        }
        return reply;
    }

    private void tracePrompt(
            AgentTraceCollector trace,
            int messageCount,
            String toolSupplement,
            String memorySupplement,
            String draftExcerpt) {
        boolean hasTool = toolSupplement != null && !toolSupplement.isBlank();
        boolean hasMemory = memorySupplement != null && !memorySupplement.isBlank();
        boolean hasDraft = draftExcerpt != null && !draftExcerpt.isBlank();
        traceOf(trace, t -> t.prompt(messageCount, hasTool, hasMemory, hasDraft));
    }

    private void traceOf(AgentTraceCollector trace, Consumer<AgentTraceCollector> action) {
        if (trace == null) {
            return;
        }
        try {
            action.accept(trace);
        } catch (RuntimeException ex) {
            log.warn("agent trace collect failed sessionId={} cause={}",
                    trace.sessionId(), ex.getClass().getSimpleName());
        }
    }

    private void logProviderIssue(
            String operation,
            AgentStage stage,
            long startedAt,
            AgentProviderFailureCategory category) {
        log.warn(
                "Agent provider issue: operation={} stage={} provider={} durationMs={} category={} transient={}",
                operation,
                stage,
                modelClient.provider(),
                millisSince(startedAt),
                category.wireId(),
                category.isTransient());
    }

    private static long millisSince(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private static Long traceSessionId(AgentTraceCollector trace) {
        return trace == null ? null : trace.sessionId();
    }

    private static Integer traceTurnNo(AgentTraceCollector trace) {
        return trace == null ? null : trace.turnNo();
    }

    private static String latestUserContent(List<AgentMessage> history) {
        if (history == null) {
            return "";
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            AgentMessage message = history.get(i);
            if (message.getRole() == AgentMessageRole.USER) {
                return message.getContent();
            }
        }
        return "";
    }

    private record GuardedReply(
            String value,
            AgentTraceLayer layer,
            AgentGuardrailVerdict verdict) {

        static GuardedReply passed(String value) {
            return new GuardedReply(value, null, AgentGuardrailVerdict.pass());
        }

        static GuardedReply failed(
                String value, AgentTraceLayer layer, AgentGuardrailVerdict verdict) {
            return new GuardedReply(value, layer, verdict);
        }

        boolean passed() {
            return verdict != null && verdict.isPassed();
        }

        com.flashback.agent.guardrail.AgentGuardrailViolation violation() {
            return verdict == null ? null : verdict.violation();
        }
    }
}
