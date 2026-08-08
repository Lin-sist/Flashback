package com.flashback.agent.eval;

import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentProviderException;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** C8：在 C6 离线评测边界驱动类型化故障与可控 deadline。 */
class AgentResilienceEvalTest {

    @Test
    void scriptedFailuresShouldReachTraceAsStableCategoriesWithoutAssistantPersistence() {
        for (AgentProviderFailureCategory category : AgentProviderFailureCategory.values()) {
            AgentEvalHarness harness = AgentEvalHarness.builder().build();
            harness.client().scriptReply(ScriptedAgentModelClient.Scripted.failure(category));

            harness.turn("今天心里有点乱");

            assertThat(harness.sink().last().causeType()).isEqualTo(category.wireId());
            assertThat(harness.messages())
                    .as("失败模板只能放在 VO.message，不能持久化为 Assistant")
                    .hasSize(2);
        }
    }

    @Test
    void fakeClockShouldExhaustBudgetBeforeScriptedProviderConsumesACall() {
        AppAgentProperties properties = new AppAgentProperties();
        ScriptedAgentModelClient client = ScriptedAgentModelClient.available(properties)
                .scriptReply(ScriptedAgentModelClient.Scripted.reply("不会被消费"));
        AtomicLong now = new AtomicLong();
        AgentCallBudget budget = AgentCallBudget.start(24_000, now::get);
        now.set(23_950_000_000L);

        assertThatThrownBy(() -> client.completeWithTools(List.of(), List.of(), false, budget))
                .isInstanceOfSatisfying(AgentProviderException.class,
                        ex -> assertThat(ex.category()).isEqualTo(AgentProviderFailureCategory.TIMEOUT));
        assertThat(client.replyCallCount()).isZero();
    }
}
