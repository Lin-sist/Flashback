package com.flashback.agent.guardrail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentQuestionLimitPolicyTest {

    private final AgentQuestionLimitPolicy policy = new AgentQuestionLimitPolicy();

    @Test
    void mustCountChineseEnglishAndMixedQuestionMarkRuns() {
        assertThat(policy.countQuestions("你想先说哪一件？Is that okay? 还是先停下？？?"))
                .isEqualTo(3);
    }

    @Test
    void mustEnforceZeroOrOneFromBackendPolicy() {
        assertThat(policy.check("我听见了。", 0).isPassed()).isTrue();
        assertThat(policy.check("你想先看哪一件？", 1).isPassed()).isTrue();
        AgentGuardrailVerdict violation = policy.check("你想先看哪一件？还是先说另一件？", 1);
        assertThat(violation.violation()).isEqualTo(AgentGuardrailViolation.EXCESSIVE_QUESTIONS);
        assertThat(violation.maxUncoveredRun()).isEqualTo(2);
    }

    @Test
    void backendOwnedFallbackMustBeSafeForZeroQuestionTurns() {
        assertThat(policy.check(AgentGuardrailRules.SAFE_FALLBACK_REPLY, 0).isPassed()).isTrue();
    }
}
