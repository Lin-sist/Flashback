package com.flashback.agent.trace;

import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 版本锚点单测（C5，design 决策 6）。
 *
 * 本类只守一件事，但它是版本字段存在的**全部意义**：
 * 改了会进入模型输入的内容，版本值必须自动变化。
 *
 * 手工维护版本常量的失效方式是「改了忘 bump」，
 * 结果 C6 拿到「版本没变但行为已变」的脏数据——那比没有版本更糟，
 * 因为没有版本时你知道自己不知道。
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentTraceVersionsTest {

    @Autowired
    private AgentTraceVersions versions;

    @Autowired
    private AppAgentProperties properties;

    @Test
    void versionsShouldBeStableWhenNothingChanges() {
        assertThat(versions.promptVersion()).isEqualTo(versions.promptVersion());
        assertThat(versions.policyVersion()).isEqualTo(versions.policyVersion());
    }

    @Test
    void versionsShouldBePrefixedAndShort() {
        assertThat(versions.promptVersion()).startsWith("p").hasSize(9);
        assertThat(versions.policyVersion()).startsWith("g").hasSize(9);
    }

    @Test
    void promptAndPolicyVersionsShouldBeIndependent() {
        assertThat(versions.promptVersion()).isNotEqualTo(versions.policyVersion());
    }

    /**
     * 护栏文案变化必须反映到 policyVersion。
     *
     * 用回复长度上限作为触发器：它进 system prompt 的「回复长度硬上限」那一行，
     * 属于 guardrailClause 的组成部分，是最容易被当成「纯参数」而漏进版本的一项。
     */
    @Test
    void policyVersionShouldChangeWhenGuardrailTextChanges() {
        String before = versions.policyVersion();
        int original = properties.getMaxReplyChars();
        properties.setMaxReplyChars(original + 7);
        try {
            assertThat(versions.policyVersion())
                    .as("护栏条款文案变了，版本必须跟着变——否则 C6 会拿到脏版本")
                    .isNotEqualTo(before);
        } finally {
            properties.setMaxReplyChars(original);
        }
        assertThat(versions.policyVersion())
                .as("还原后版本应回到原值，说明它确实由内容派生而非累加")
                .isEqualTo(before);
    }

    @Test
    void promptVersionShouldNotBeAffectedByGuardrailThresholds() {
        // 提示词版本只由 prompt 侧文案派生。忠实度阈值不进 prompt，
        // 因此改它不该动 promptVersion——否则两个版本号会互相污染，
        // C6 无法区分「改了话术」与「改了阈值」。
        String before = versions.promptVersion();
        int original = properties.getGuardrail().getMaxUncoveredRun();
        properties.getGuardrail().setMaxUncoveredRun(original + 3);
        try {
            assertThat(versions.promptVersion()).isEqualTo(before);
        } finally {
            properties.getGuardrail().setMaxUncoveredRun(original);
        }
    }
}
