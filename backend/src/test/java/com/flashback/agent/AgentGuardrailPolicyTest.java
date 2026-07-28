package com.flashback.agent;

import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentGuardrailPolicyTest {

    private AgentGuardrailPolicy policyWithLimit(int limit) {
        AppAgentProperties properties = new AppAgentProperties();
        properties.setMaxReplyChars(limit);
        // C4：护栏文案改由 AgentGuardrailRules 单一声明源提供（design 决策 5）。
        return new AgentGuardrailPolicy(properties, new AgentGuardrailRules());
    }

    @Test
    void shouldKeepShortReplyUnchanged() {
        AgentGuardrailPolicy policy = policyWithLimit(120);

        assertThat(policy.enforceReplyLength("  今天是什么让你想写下这一刻？  "))
                .isEqualTo("今天是什么让你想写下这一刻？");
    }

    @Test
    void shouldTrimReplyAtSentenceBoundaryWhenTooLong() {
        AgentGuardrailPolicy policy = policyWithLimit(20);

        String result = policy.enforceReplyLength("听起来不太容易。你想先说哪一part分呢，还是先停一下休息会儿");

        assertThat(result).isEqualTo("听起来不太容易。");
        assertThat(result.length()).isLessThanOrEqualTo(20);
    }

    @Test
    void shouldHardTruncateWhenNoSentenceBoundaryExists() {
        AgentGuardrailPolicy policy = policyWithLimit(10);

        String result = policy.enforceReplyLength("这是一段没有任何标点符号的很长的回复内容");

        assertThat(result).hasSize(10);
    }

    @Test
    void shouldExposeFiveMinimumGuardrailsInClause() {
        AgentGuardrailPolicy policy = policyWithLimit(120);

        String clause = policy.guardrailClause();

        assertThat(AgentGuardrailPolicy.MINIMUM_GUARDRAILS).hasSize(5);
        assertThat(clause).contains("不诊断", "不覆写", "建议不代决", "被动陪伴", "输出克制");
    }

    @Test
    void shouldHandleNullReply() {
        assertThat(policyWithLimit(120).enforceReplyLength(null)).isNull();
    }
}
