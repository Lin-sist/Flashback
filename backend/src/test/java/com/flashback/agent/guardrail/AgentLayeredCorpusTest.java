package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 分层来源集合测试（C3 agent-memory-retrieval）。
 *
 * 本类固定三件事，每一件都对应一条会被后续改动破坏的不变量：
 * 1. 两层权限不对等——会话层是唯一可进正文的来源；
 * 2. 记忆层只含**本轮实际注入**的片段（不变量 1）；
 * 3. 「只来自记忆层」的片段可被精确识别（时间归属检查的前提）。
 */
class AgentLayeredCorpusTest {

    private static final int NGRAM = new AppAgentProperties().getGuardrail().getFaithfulnessNgramSize();

    private static final String SESSION_SAID = "今天开会又被临时加了需求，有点烦";
    private static final String MEMORY_SAID = "三月那次项目截止日期压得我喘不过气";
    private static final String NOT_INJECTED = "去年冬天我在考虑要不要换城市生活";

    private AgentMessage user(String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(AgentMessageRole.USER);
        message.setContent(content);
        return message;
    }

    private List<AgentMessage> history(String... contents) {
        List<AgentMessage> history = new ArrayList<>();
        for (String content : contents) {
            history.add(user(content));
        }
        return history;
    }

    // ---------- 分层构造 ----------

    @Test
    void shouldExposeBothLayersWhenMemoryInjected() {
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        assertThat(corpus.hasMemory()).isTrue();
        assertThat(corpus.sessionOnly().isEmpty()).isFalse();
        assertThat(corpus.memoryOnly().isEmpty()).isFalse();
    }

    @Test
    void shouldDegradeToSessionOnlyWhenNoFragmentsInjected() {
        // 检索无命中 / 检索失败 / 记忆开关关闭都会走到这里，
        // 此时行为必须等价于 C4 现状：没有记忆层，判定照旧严格。
        AgentLayeredCorpus nullFragments = AgentLayeredCorpus.of(history(SESSION_SAID), null, NGRAM);
        AgentLayeredCorpus emptyFragments = AgentLayeredCorpus.of(history(SESSION_SAID), List.of(), NGRAM);
        AgentLayeredCorpus blankFragments = AgentLayeredCorpus.of(history(SESSION_SAID), List.of("   "), NGRAM);

        assertThat(nullFragments.hasMemory()).isFalse();
        assertThat(emptyFragments.hasMemory()).isFalse();
        assertThat(blankFragments.hasMemory()).isFalse();
        assertThat(nullFragments.longestMemoryOnlyRun(MEMORY_SAID)).isZero();
    }

    @Test
    void sessionOnlyFactoryShouldToleratePlainCorpus() {
        AgentSourceCorpus session = AgentSourceCorpus.of(history(SESSION_SAID), NGRAM);

        AgentLayeredCorpus corpus = AgentLayeredCorpus.sessionOnly(session);

        assertThat(corpus.hasMemory()).isFalse();
        assertThat(corpus.combined()).isSameAs(session);
    }

    // ---------- 不变量 1：只含本轮实际注入的片段 ----------

    @Test
    void memoryLayerMustOnlyContainInjectedFragments() {
        // MEMORY_SAID 注入了，NOT_INJECTED 只是「存在于用户历史里」但本轮没注入。
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        AgentCoverageProfile injectedProfile = AgentCoverageProfile.of(MEMORY_SAID, corpus.combined());
        AgentCoverageProfile notInjectedProfile = AgentCoverageProfile.of(NOT_INJECTED, corpus.combined());

        assertThat(injectedProfile.coverage()).isEqualTo(1.0d);
        assertThat(notInjectedProfile.coverage())
                .as("未注入的历史内容不得成为合法来源，否则忠实度闸退化成「用户这辈子说过就放行」")
                .isLessThan(0.2d);
    }

    // ---------- 权限不对等 ----------

    @Test
    void sessionLayerMustNotCoverMemoryContent() {
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        AgentCoverageProfile viaSession = AgentCoverageProfile.of(MEMORY_SAID, corpus.sessionOnly());

        assertThat(viaSession.coverage())
                .as("正文只认会话层：记忆内容在会话层必须几乎无覆盖")
                .isLessThan(0.2d);
    }

    // ---------- 不变量：memory-only 片段可被识别 ----------

    @Test
    void shouldDetectMemoryOnlyRunForVerbatimMemoryRestatement() {
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        int run = corpus.longestMemoryOnlyRun("你说过项目截止日期压得你喘不过气");

        assertThat(run)
                .as("复述记忆原话时必须能算出足够长的 memory-only 片段，否则时间归属检查无从生效")
                .isGreaterThanOrEqualTo(8);
    }

    @Test
    void shouldReportNoMemoryOnlyRunWhenAgentOnlyUsesSessionContent() {
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        int run = corpus.longestMemoryOnlyRun("今天开会又被临时加了需求");

        assertThat(run).isZero();
    }

    @Test
    void shouldNotCountSharedPhrasesAsMemoryOnly() {
        // 用户此刻和过去用了同一个短语：该片段两层都覆盖，不属于 memory-only。
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history("最近压力有点大"), List.of("那时候压力有点大"), NGRAM);

        int run = corpus.longestMemoryOnlyRun("压力有点大");

        assertThat(run).isZero();
    }

    // ---------- merge 的边界 ----------

    @Test
    void mergeShouldRejectMismatchedNgramSizes() {
        AgentSourceCorpus four = AgentSourceCorpus.ofTexts(List.of(SESSION_SAID), 4);
        AgentSourceCorpus three = AgentSourceCorpus.ofTexts(List.of(MEMORY_SAID), 3);

        assertThatThrownBy(() -> AgentSourceCorpus.merge(four, three))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeShouldReturnOtherSideWhenOneIsEmpty() {
        AgentSourceCorpus filled = AgentSourceCorpus.ofTexts(List.of(SESSION_SAID), NGRAM);
        AgentSourceCorpus empty = AgentSourceCorpus.ofTexts(List.of(), NGRAM);

        assertThat(AgentSourceCorpus.merge(filled, empty)).isSameAs(filled);
        assertThat(AgentSourceCorpus.merge(empty, filled)).isSameAs(filled);
    }

    @Test
    void mergedCorpusShouldCoverBothLayers() {
        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                history(SESSION_SAID), List.of(MEMORY_SAID), NGRAM);

        assertThat(AgentCoverageProfile.of(SESSION_SAID, corpus.combined()).coverage()).isEqualTo(1.0d);
        assertThat(AgentCoverageProfile.of(MEMORY_SAID, corpus.combined()).coverage()).isEqualTo(1.0d);
    }
}
