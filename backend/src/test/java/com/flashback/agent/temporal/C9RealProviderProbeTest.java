package com.flashback.agent.temporal;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** C9 闸门 3：真实 provider 的六个固定合成时间场景；不打印 prompt 或回复。 */
@EnabledIfEnvironmentVariable(named = "C9_REAL_PROBE", matches = "1")
class C9RealProviderProbeTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void sixSyntheticTemporalScenariosMustStayRestrainedWithOneCallEach() throws Exception {
        AppAgentProperties properties = new AppAgentProperties();
        AgentGuardrailRules rules = new AgentGuardrailRules();
        AgentGuardrailPolicy guardrail = new AgentGuardrailPolicy(properties, rules);
        AgentPromptBuilder prompt = new AgentPromptBuilder(properties, guardrail, rules);
        AgentTemporalPolicy temporal = new AgentTemporalPolicy(properties, CLOCK);
        AgentTemporalLanguageChecker checker = new AgentTemporalLanguageChecker();
        AgentModelClient client = realClient(properties);

        List<Scenario> scenarios = List.of(
                scenario("recent", AgentChatMode.WRITING_GUIDANCE, "最近又想到方向问题", List.of(),
                        List.of(fragment(1, "2026-07-20T10:00:00"))),
                scenario("distant", AgentChatMode.WRITING_GUIDANCE, "最近又想到方向问题", List.of(),
                        List.of(fragment(2, "2026-04-01T10:00:00"))),
                scenario("long-ago", AgentChatMode.WRITING_GUIDANCE, "最近又想到方向问题", List.of(),
                        List.of(fragment(3, "2025-01-01T10:00:00"))),
                scenario("review-focal", AgentChatMode.REVIEW_CHAT, "现在想回头看看",
                        List.of(fragment(4, "2025-01-01T10:00:00")), List.of()),
                scenario("recurrence-eligible", AgentChatMode.REVIEW_CHAT, "以前也有过类似的时候吗", List.of(),
                        List.of(fragment(5, "2026-01-01T10:00:00"), fragment(6, "2026-05-01T10:00:00"))),
                scenario("recurrence-insufficient", AgentChatMode.REVIEW_CHAT, "今天只想看看那时候", List.of(),
                        List.of(fragment(7, "2026-01-01T10:00:00"), fragment(8, "2026-05-01T10:00:00"))));

        int calls = 0;
        for (Scenario scenario : scenarios) {
            TemporalPolicyResult result = temporal.evaluate(
                    scenario.mode(), scenario.input(), scenario.focal(), scenario.ancillary());
            String supplement = prompt.buildMemorySupplement(result.injectedFragments()) + "\n\n"
                    + prompt.buildTemporalSupplement(result);
            AgentCallBudget budget = AgentCallBudget.start(24_000L);
            AgentModelResponse response = client.completeWithTools(
                    prompt.buildConversationMessages(stageOf(scenario.mode()),
                            List.of(userMessage(scenario.input())), null, null, supplement),
                    List.of(), false, budget);
            calls++;
            String reply = guardrail.enforceReplyLength(prompt.normalizeReplyShape(response.content()));
            assertThat(reply).isNotBlank();
            assertThat(checker.check(reply, result.patternEvidence().eligible()).isPassed()).isTrue();
            assertThat(budget.isExhausted()).isFalse();
            System.out.printf("C9PROBE scenario=%s call=1 eligible=%s hintUsed=%s success=true%n",
                    scenario.id(), result.patternEvidence().eligible(), reply.contains("似乎不止一次"));
        }
        assertThat(calls).isEqualTo(6);
        System.out.printf("C9PROBE DONE totalCalls=%d maxAllowed=6%n", calls);
    }

    private AgentModelClient realClient(AppAgentProperties properties) {
        AppAiProperties ai = new AppAiProperties();
        ai.setProvider(System.getenv().getOrDefault("AI_PROVIDER", "mock"));
        ai.setBaseUrl(System.getenv().getOrDefault("AI_BASE_URL", "https://api.deepseek.com"));
        ai.setApiKey(System.getenv().getOrDefault("AI_API_KEY", ""));
        ai.setModel(System.getenv().getOrDefault("AI_MODEL", "deepseek-v4-pro"));
        ai.setTimeoutMillis(20_000L);
        AgentModelClient client = new AgentModelClient(ai, properties);
        assertThat(client.isMockProvider()).isFalse();
        assertThat(client.unavailableReason()).isNull();
        return client;
    }

    private MemoryFragment fragment(long id, String at) {
        return new MemoryFragment(id, LocalDateTime.parse(at), at.substring(0, 7),
                "这是固定合成的方向犹豫片段，只用于 C9 验收，不含真实用户内容。".repeat(3));
    }

    private AgentMessage userMessage(String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(AgentMessageRole.USER);
        message.setStage(AgentStage.REVIEW);
        message.setContent(content);
        return message;
    }

    private AgentStage stageOf(AgentChatMode mode) {
        return mode == AgentChatMode.REVIEW_CHAT ? AgentStage.REVIEW : AgentStage.CONFUSION;
    }

    private Scenario scenario(String id, AgentChatMode mode, String input,
                              List<MemoryFragment> focal, List<MemoryFragment> ancillary) {
        return new Scenario(id, mode, input, focal, ancillary);
    }

    private record Scenario(String id, AgentChatMode mode, String input,
                            List<MemoryFragment> focal, List<MemoryFragment> ancillary) {}
}
