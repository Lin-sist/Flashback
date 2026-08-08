package com.flashback.agent.resilience;

import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * C8 闸门 3：真实 provider 下验证单调用与双调用共享 deadline。
 *
 * <p>固定合成输入；不打印回复、prompt、凭证或 endpoint。最多 6 次真实调用。</p>
 */
@EnabledIfEnvironmentVariable(named = "C8_REAL_PROBE", matches = "1")
class C8RealProviderProbeTest {

    private static final int CANARY_CALLS = 2;
    private static final int DOUBLE_CALL_RUNS = 2;

    @Test
    void probeSingleAndDoubleCallsWithinSharedBudget() throws Exception {
        AppAiProperties ai = new AppAiProperties();
        ai.setProvider(System.getenv().getOrDefault("AI_PROVIDER", "mock"));
        ai.setBaseUrl(System.getenv().getOrDefault("AI_BASE_URL", "https://api.deepseek.com"));
        ai.setApiKey(System.getenv().getOrDefault("AI_API_KEY", ""));
        ai.setModel(System.getenv().getOrDefault("AI_MODEL", "deepseek-v4-pro"));
        ai.setTimeoutMillis(20_000L);

        AppAgentProperties agent = new AppAgentProperties();
        AgentModelClient client = new AgentModelClient(ai, agent);
        assertThat(client.isMockProvider()).isFalse();
        assertThat(client.unavailableReason()).isNull();

        int calls = 0;
        for (int i = 1; i <= CANARY_CALLS; i++) {
            AgentCallBudget budget = AgentCallBudget.start(24_000L);
            long started = System.nanoTime();
            AgentModelResponse response = client.completeWithTools(messages("canary-" + i), List.of(), false, budget);
            calls++;
            long elapsedMs = elapsedMillis(started);
            assertThat(response.content()).isNotBlank();
            assertThat(elapsedMs).isLessThan(30_000L);
            assertThat(budget.isExhausted()).isFalse();
            System.out.printf(
                    "C8PROBE canary=%d calls=1 elapsedMs=%d remainingBucket=%s success=true%n",
                    i, elapsedMs, budget.remainingBucket());
        }

        for (int run = 1; run <= DOUBLE_CALL_RUNS; run++) {
            AgentCallBudget budget = AgentCallBudget.start(24_000L);
            long started = System.nanoTime();
            AgentModelResponse initial = client.completeWithTools(
                    messages("double-" + run + "-initial"), List.of(), false, budget);
            calls++;
            AgentModelResponse reflection = client.completeWithTools(
                    messages("double-" + run + "-reflection"), List.of(), false, budget);
            calls++;
            long elapsedMs = elapsedMillis(started);
            assertThat(initial.content()).isNotBlank();
            assertThat(reflection.content()).isNotBlank();
            assertThat(elapsedMs).isLessThan(30_000L);
            assertThat(budget.isExhausted()).isFalse();
            System.out.printf(
                    "C8PROBE doubleRun=%d calls=2 elapsedMs=%d remainingBucket=%s success=true%n",
                    run, elapsedMs, budget.remainingBucket());
        }

        assertThat(calls).isEqualTo(6);
        System.out.printf("C8PROBE DONE totalCalls=%d maxAllowed=8%n", calls);
    }

    private List<Map<String, String>> messages(String marker) {
        return List.of(
                Map.of("role", "system", "content", "只回复一句简短、克制的中文确认，不添加事实。"),
                Map.of("role", "user", "content", "这是韧性验收合成输入：" + marker));
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }
}
