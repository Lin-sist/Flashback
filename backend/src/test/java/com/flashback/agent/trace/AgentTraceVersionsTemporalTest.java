package com.flashback.agent.trace;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.reflection.AgentReflectionPolicy;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTraceVersionsTemporalTest {

    @Test
    void temporalThresholdChangeMustChangePolicyFingerprint() {
        AppAgentProperties properties = new AppAgentProperties();
        AgentGuardrailRules rules = new AgentGuardrailRules();
        AgentGuardrailPolicy guardrail = new AgentGuardrailPolicy(properties, rules);
        AgentPromptBuilder prompt = new AgentPromptBuilder(properties, guardrail, rules);
        AgentTraceVersions versions = new AgentTraceVersions(
                prompt, guardrail, rules, new AgentReflectionPolicy(), properties);

        String before = versions.policyVersion();
        properties.getTemporal().setRecurrenceMinSpanDays(91);

        assertThat(versions.policyVersion()).isNotEqualTo(before);
    }
}
