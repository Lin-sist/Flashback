package com.flashback.agent.resilience;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentCallBudgetTest {

    @Test
    void shouldCapEachCallByPerCallLimitAndSharedRemainingBudget() throws Exception {
        AtomicLong nanos = new AtomicLong();
        AgentCallBudget budget = AgentCallBudget.start(24_000L, nanos::get);

        assertThat(budget.nextCallTimeoutMillis(20_000L)).isEqualTo(20_000L);
        nanos.set(10_000_000_000L);
        assertThat(budget.nextCallTimeoutMillis(20_000L)).isEqualTo(14_000L);
        assertThat(budget.remainingBucket()).isEqualTo("gt-5s");
    }

    @Test
    void shouldFailBeforeCallingProviderWhenSafeMinimumIsGone() {
        AtomicLong nanos = new AtomicLong();
        AgentCallBudget budget = AgentCallBudget.start(24_000L, nanos::get);
        nanos.set(23_950_000_000L);

        assertThatThrownBy(() -> budget.nextCallTimeoutMillis(20_000L))
                .isInstanceOf(AgentProviderException.class)
                .satisfies(error -> assertThat(((AgentProviderException) error).category())
                        .isEqualTo(AgentProviderFailureCategory.TIMEOUT));
        assertThat(budget.isExhausted()).isTrue();
        assertThat(budget.remainingBucket()).isEqualTo("lt-1s");
    }

    @Test
    void shouldRejectInvalidBudgets() {
        assertThatThrownBy(() -> AgentCallBudget.start(0L, System::nanoTime))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
