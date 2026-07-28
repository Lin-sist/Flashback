package com.flashback.agent.guardrail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 归一化等价性测试（C4）。
 *
 * 归一化的作用是让「同一句话的不同书写形式」得到同一结果，
 * 否则模型只要改个标点就会让覆盖率虚低，正常整理被误判成增写。
 */
class AgentTextNormalizerTest {

    @Test
    void shouldTreatPunctuationVariantsAsEquivalent() {
        String base = AgentTextNormalizer.normalize("我最近睡不好");

        assertThat(AgentTextNormalizer.normalize("我最近睡不好。")).isEqualTo(base);
        assertThat(AgentTextNormalizer.normalize("我最近，睡不好！")).isEqualTo(base);
        assertThat(AgentTextNormalizer.normalize("我最近……睡不好？？")).isEqualTo(base);
    }

    @Test
    void shouldTreatWhitespaceVariantsAsEquivalent() {
        String base = AgentTextNormalizer.normalize("我最近睡不好");

        assertThat(AgentTextNormalizer.normalize("我最近 睡不好")).isEqualTo(base);
        assertThat(AgentTextNormalizer.normalize("  我最近\n睡不好\t")).isEqualTo(base);
        assertThat(AgentTextNormalizer.normalize("我最近　睡不好")).isEqualTo(base);
    }

    @Test
    void shouldFoldFullWidthAndCaseForLatin() {
        assertThat(AgentTextNormalizer.normalize("ＰＴＳＤ")).isEqualTo("ptsd");
        assertThat(AgentTextNormalizer.normalize("PTSD")).isEqualTo("ptsd");
        assertThat(AgentTextNormalizer.normalize("Ptsd")).isEqualTo("ptsd");
    }

    @Test
    void shouldKeepDigits() {
        assertThat(AgentTextNormalizer.normalize("3 月 15 号")).isEqualTo("3月15号");
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        assertThat(AgentTextNormalizer.normalize(null)).isEmpty();
        assertThat(AgentTextNormalizer.normalize("")).isEmpty();
        assertThat(AgentTextNormalizer.normalize("  \n\t")).isEmpty();
        assertThat(AgentTextNormalizer.normalize("。，！？")).isEmpty();
    }

    @Test
    void shouldBeDeterministic() {
        String input = "我学的是软件工程，一直想做后端！";

        assertThat(AgentTextNormalizer.normalize(input))
                .isEqualTo(AgentTextNormalizer.normalize(input));
    }
}
