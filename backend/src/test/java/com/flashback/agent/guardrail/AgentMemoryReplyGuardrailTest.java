package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 记忆层对既有护栏层的影响（C3 agent-memory-retrieval）。
 *
 * 本类回答一个必须被固定的问题：**引入记忆层有没有把 C4 的护栏改松。**
 * 每个用例都是一条「不得放宽」的断言，而不是新能力的展示。
 */
class AgentMemoryReplyGuardrailTest {

    private static final String SESSION_SAID = "今天开会又被临时加了需求，有点烦";
    private static final String MEMORY_SAID = "三月那次项目截止日期压得我喘不过气";

    private AppAgentProperties properties;
    private AgentFaithfulnessChecker faithfulnessChecker;
    private AgentContentChecker contentChecker;
    private AgentTimeAttributionChecker timeAttributionChecker;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        contentChecker = new AgentContentChecker(properties, faithfulnessChecker);
        timeAttributionChecker = new AgentTimeAttributionChecker(properties);
    }

    private AgentMessage user(String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(AgentMessageRole.USER);
        message.setContent(content);
        return message;
    }

    private AgentLayeredCorpus withMemory() {
        return AgentLayeredCorpus.of(
                List.of(user(SESSION_SAID)), List.of(MEMORY_SAID),
                properties.getGuardrail().getFaithfulnessNgramSize());
    }

    // ---------- 阈值未被放宽 ----------

    @Test
    void guardrailThresholdsMustKeepTheirCalibratedDefaults() {
        AppAgentProperties.Guardrail guardrail = properties.getGuardrail();

        // 这些值是 C4 用真实样本标定的；C3 不得为了让记忆「更好用」而调松它们。
        assertThat(guardrail.getMinCoverage()).isEqualTo(0.35d);
        assertThat(guardrail.getMaxUncoveredRun()).isEqualTo(12);
        assertThat(guardrail.getMinCheckedLength()).isEqualTo(12);
        assertThat(guardrail.isFaithfulnessEnabled()).isTrue();
        assertThat(guardrail.isContentCheckEnabled()).isTrue();
    }

    // ---------- 记忆层不得放宽正文判定 ----------

    @Test
    void memoryMustNotMakeContentFaithfulnessMorePermissive() {
        AgentLayeredCorpus corpus = withMemory();

        // 同一段记忆文本：会话层判不忠实（正确），合并层判忠实（说明它确实来自记忆）。
        assertThat(faithfulnessChecker.check(MEMORY_SAID, corpus.sessionOnly()).isPassed()).isFalse();
        assertThat(faithfulnessChecker.check(MEMORY_SAID, corpus.combined()).isPassed()).isTrue();
    }

    // ---------- 伪引用严判对两层都生效 ----------

    @Test
    void fabricatedQuoteMustStillBeRejectedWithMemoryPresent() {
        AgentLayeredCorpus corpus = withMemory();

        AgentGuardrailVerdict verdict = contentChecker.checkQuotes(
                "你说过“我已经彻底放弃找工作了”，是这样吗", corpus.combined());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.FABRICATED_QUOTE);
    }

    @Test
    void genuineMemoryQuoteShouldPassQuoteCheck() {
        AgentLayeredCorpus corpus = withMemory();

        AgentGuardrailVerdict verdict = contentChecker.checkQuotes(
                "三月那次你写过“项目截止日期压得我喘不过气”", corpus.combined());

        assertThat(verdict.isPassed()).isTrue();
    }

    // ---------- 诊断与代决检查不受记忆影响 ----------

    @Test
    void diagnosticCheckMustStillFireWithMemoryPresent() {
        AgentLayeredCorpus corpus = withMemory();

        AgentGuardrailVerdict verdict = contentChecker.check(
                "你这是典型的焦虑症表现，建议你就医", corpus.combined());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.DIAGNOSTIC);
    }

    @Test
    void fakeActionCheckMustStillFireWithMemoryPresent() {
        AgentLayeredCorpus corpus = withMemory();

        AgentGuardrailVerdict verdict = contentChecker.check(
                "我已经帮你封存好了，放心吧", corpus.combined());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.FAKE_ACTION);
    }

    // ---------- 时间归属是新增的一层，不替代任何既有层 ----------

    @Test
    void timeAttributionIsAdditiveNotAReplacement() {
        AgentLayeredCorpus corpus = withMemory();
        // 一句既缺时间归属、又不含任何诊断词的复述：
        // 内容检查放行（它管的不是这件事），时间归属拦下。
        String reply = "项目截止日期压得你喘不过气，这次也一样吗";

        assertThat(contentChecker.check(reply, corpus.combined()).isPassed()).isTrue();
        assertThat(timeAttributionChecker.check(reply, corpus).isPassed()).isFalse();
    }

    @Test
    void timeAttributionMustNotFireWithoutMemoryLayer() {
        AgentLayeredCorpus sessionOnly = AgentLayeredCorpus.of(
                List.of(user(SESSION_SAID)), List.of(),
                properties.getGuardrail().getFaithfulnessNgramSize());

        assertThat(timeAttributionChecker.check(
                "项目截止日期压得你喘不过气", sessionOnly).isPassed()).isTrue();
    }

    // ---------- 素材路径仍只认会话层 ----------

    @Test
    void materialFaithfulnessMustUseSessionLayerOnly() {
        AgentLayeredCorpus corpus = withMemory();

        // 素材若由记忆内容构成，用会话层判定必须不通过——
        // 正文只能来自本次对话的表达（不变量 2）。
        assertThat(faithfulnessChecker.check(MEMORY_SAID, corpus.sessionOnly()).isPassed()).isFalse();
        // 而本次说过的话仍然通过。
        assertThat(faithfulnessChecker.check(SESSION_SAID, corpus.sessionOnly()).isPassed()).isTrue();
    }
}
