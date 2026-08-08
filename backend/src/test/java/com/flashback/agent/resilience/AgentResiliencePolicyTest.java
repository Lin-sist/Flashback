package com.flashback.agent.resilience;

import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentResiliencePolicyTest {

    private final AgentResiliencePolicy policy = new AgentResiliencePolicy();

    @Test
    void shouldUseWarmFixedOpeningMessageWithoutInfrastructureTerms() {
        String message = policy.failureMessage("opening", AgentStage.OPENING,
                AgentProviderFailureCategory.TIMEOUT);

        assertThat(message).isEqualTo("我现在还没能接上，可以稍后再试一次。");
        assertThat(message).doesNotContain("provider", "401", "鉴权", "配置", "timeout");
    }

    @Test
    void shouldTellTurnUserThatSubmittedTextWasKept() {
        String message = policy.failureMessage("turn", AgentStage.CONFUSION,
                AgentProviderFailureCategory.TIMEOUT);

        assertThat(message).isEqualTo("刚才写下的这句还在。我现在没能接上，可以再试一次。");
    }

    @Test
    void shouldKeepMessagesIndependentFromFailureDetails() {
        assertThat(policy.failureMessage("turn", AgentStage.EMOTION,
                AgentProviderFailureCategory.THROTTLED))
                .isEqualTo(policy.failureMessage("turn", AgentStage.EMOTION,
                        AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE));
    }

    @Test
    void shouldNotEncourageImmediateRetryForNonTransientFailure() {
        assertThat(policy.failureMessage("opening", AgentStage.OPENING,
                AgentProviderFailureCategory.AUTH_CONFIGURATION))
                .isEqualTo("我现在暂时无法接上，请稍后再回来。");
        assertThat(policy.failureMessage("turn", AgentStage.CLOSING,
                AgentProviderFailureCategory.INVALID_RESPONSE))
                .isEqualTo("刚才写下的这句还在，但现在暂时无法继续。");
    }
}
