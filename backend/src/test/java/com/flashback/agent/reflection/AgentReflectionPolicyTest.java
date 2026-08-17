package com.flashback.agent.reflection;

import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentReflectionPolicyTest {

    private final AgentReflectionPolicy policy = new AgentReflectionPolicy();

    @Test
    void shouldOnlyAllowMissingTimeAttributionOutsideClosing() {
        for (AgentGuardrailViolation violation : AgentGuardrailViolation.values()) {
            boolean expected = violation == AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION
                    || violation == AgentGuardrailViolation.EXCESSIVE_QUESTIONS;
            assertThat(policy.instructionFor(AgentStage.EMOTION, violation).isPresent())
                    .as("violation=%s", violation)
                    .isEqualTo(expected);
        }

        assertThat(policy.instructionFor(
                AgentStage.CLOSING, AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION)).isEmpty();
        assertThat(policy.instructionFor(
                null, AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION)).isEmpty();
        assertThat(policy.instructionFor(AgentStage.EMOTION, null)).isEmpty();
        assertThat(AgentReflectionPolicy.MAX_REFLECTION_REWRITES).isEqualTo(1);
    }

    @Test
    void fixedInstructionAndFingerprintShouldContainNoRuntimeText() {
        String instruction = policy.instructionFor(
                AgentStage.EMOTION, AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION).orElseThrow();

        assertThat(instruction)
                .contains("过去某个时候")
                .doesNotContain("候选回复", "用户原话", "记忆片段");
        assertThat(policy.fingerprintSource())
                .contains(instruction)
                .contains("max-rewrites=1")
                .contains("closing=disabled");
    }
}
