package com.flashback.agent;

import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.safety.AgentSafetyDecision;
import com.flashback.agent.safety.AgentSafetyPolicy;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R1 Gate 3：固定合成 provider 边界探针。
 *
 * <p>硬预算 1：高风险正例只验证本地 policy，不允许进入 provider；唯一真实调用
 * 使用普通低落合成句，证明负例仍可走 provider。输出只记录计数，不打印 prompt/reply。</p>
 */
@EnabledIfEnvironmentVariable(named = "R1_REAL_PROBE", matches = "1")
class R1RealProviderBoundaryProbeTest {

    private static final int MAX_PROVIDER_CALLS = 1;

    @Test
    void immediateRiskMustStayLocalWhileOrdinaryDistressMayReachProvider() throws Exception {
        AgentSafetyPolicy policy = new AgentSafetyPolicy();
        AgentSafetyDecision immediate = policy.assess("我现在就要去死");
        assertThat(immediate.intervenes()).isTrue();

        AgentSafetyDecision ordinary = policy.assess("这次失败让我很迷茫");
        assertThat(ordinary.intervenes()).isFalse();

        CountingModelClient client = new CountingModelClient(
                realAiProperties(), new AppAgentProperties(), MAX_PROVIDER_CALLS);
        assertThat(client.isMockProvider()).isFalse();
        assertThat(client.unavailableReason()).isNull();

        AgentModelResponse response = client.completeWithTools(
                List.of(
                        Map.of("role", "system", "content", "请用一句克制、非诊断的话回应。"),
                        Map.of("role", "user", "content", "这次失败让我很迷茫")),
                List.of(),
                false,
                AgentCallBudget.start(24_000L));

        assertThat(response.content()).isNotBlank();
        assertThat(client.calls()).isEqualTo(1);
        System.out.printf("R1PROBE DONE safetyLocal=true ordinaryProviderCalls=%d maxAllowed=%d success=true%n",
                client.calls(), MAX_PROVIDER_CALLS);
    }

    private AppAiProperties realAiProperties() {
        AppAiProperties ai = new AppAiProperties();
        ai.setProvider(System.getenv().getOrDefault("AI_PROVIDER", "mock"));
        ai.setBaseUrl(System.getenv().getOrDefault("AI_BASE_URL", "https://api.deepseek.com"));
        ai.setApiKey(System.getenv().getOrDefault("AI_API_KEY", ""));
        ai.setModel(System.getenv().getOrDefault("AI_MODEL", "deepseek-v4-pro"));
        ai.setTimeoutMillis(20_000L);
        return ai;
    }

    private static final class CountingModelClient extends AgentModelClient {
        private final int maxCalls;
        private int calls;

        private CountingModelClient(AppAiProperties ai, AppAgentProperties agent, int maxCalls) {
            super(ai, agent);
            this.maxCalls = maxCalls;
        }

        @Override
        public AgentModelResponse completeWithTools(
                List<Map<String, String>> messages,
                List<Map<String, Object>> tools,
                boolean strictMode,
                AgentCallBudget budget) throws IOException, InterruptedException {
            assertThat(calls).as("R1 real provider hard call budget").isLessThan(maxCalls);
            calls++;
            return super.completeWithTools(messages, tools, strictMode, budget);
        }

        private int calls() {
            return calls;
        }
    }
}
