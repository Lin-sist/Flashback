package com.flashback.agent.temporal;

import com.flashback.agent.guardrail.AgentGuardrailViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTemporalLanguageCheckerTest {

    private final AgentTemporalLanguageChecker checker = new AgentTemporalLanguageChecker();

    @Test
    void shouldAllowTentativeRecurrenceAndExactMonthAttribution() {
        assertThat(checker.check("从2026年1月和5月的两段记录看，似乎不止一次。你觉得它们像吗？")
                .isPassed()).isTrue();
        assertThat(checker.check("我不想替你定义这是不是规律，你可以自己感觉。")
                .isPassed()).isTrue();
        assertThat(checker.check("你刚才说这周发生了3次，我先按你的原话听着。")
                .isPassed()).isTrue();
    }

    @Test
    void shouldBlockFrequencyCausalityDiagnosisTrendAndPrediction() {
        assertThat(checker.check("这种情况每次都会发生").violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        assertThat(checker.check("这证明你有某种问题").violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        assertThat(checker.check("未来复发率是80%").violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        assertThat(checker.check("这是越来越明显的趋势").violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        assertThat(checker.check("似乎不止一次。是的，似乎不止一次。").violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        assertThat(checker.check("似乎不止一次。你觉得呢？", false).violation())
                .isEqualTo(AgentGuardrailViolation.TEMPORAL_OVERREACH);
    }
}
