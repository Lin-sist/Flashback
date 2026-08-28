package com.flashback.agent.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSafetyPolicyTest {

    private final AgentSafetyPolicy policy = new AgentSafetyPolicy();

    @ParameterizedTest
    @ValueSource(strings = {
            "我想自杀",
            "我想死",
            "我现在就要去死",
            "如果他离开我就自杀",
            "我今晚准备结束生命",
            "我正在割腕",
            "我刚刚吞了很多药片",
            "我现在没有这种想法，但我已经吞了很多药片",
            "我真的不想活了"
    })
    void shouldInterveneForDirectImmediateSelfHarm(String input) {
        AgentSafetyDecision decision = policy.assess(input);

        assertThat(decision.intervenes()).isTrue();
        assertThat(decision.level()).isEqualTo(AgentSafetyLevel.IMMEDIATE_SELF_HARM);
        assertThat(decision.rule()).isNotEqualTo(AgentSafetyRule.NONE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "最近很低落，什么都不想做",
            "这次失败让我很迷茫",
            "今天加班快累死了",
            "我不会自杀，也没有伤害自己的打算",
            "我以前想过自杀，但现在没有",
            "朋友说他想死，我不知道怎么办",
            "新闻在讨论自杀预防",
            "如果有人说我想自杀，这句话该怎么回应",
            "我正在吃感冒药"
    })
    void shouldNotPathologizeDistressNegationHistoryOrDiscussion(String input) {
        assertThat(policy.assess(input)).isEqualTo(AgentSafetyDecision.none());
    }

    @Test
    void localResponseShouldStayNarrowAndHonest() {
        assertThat(AgentSafetyPolicy.LOCAL_RESPONSE)
                .hasSizeLessThanOrEqualTo(120)
                .contains("中国大陆", "120", "110", "12356")
                .contains("不是专业救援人员", "无法替你通知任何人")
                .doesNotContain("已经报警", "已经通知", "诊断");
        assertThat(AgentSafetyPolicy.LOCAL_RESPONSE.chars().filter(ch -> ch == '？' || ch == '?').count())
                .isLessThanOrEqualTo(1L);
    }
}
