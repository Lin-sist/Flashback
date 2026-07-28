package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 后置内容检查测试（C4）。
 *
 * 核心验收点是**分区语义**（design 决策 4）：
 * 规则只在「Agent 新增区段」匹配，用户自己说过的病症词不得误伤。
 * 这条边界决定了护栏是否与产品气质冲突——
 * 若全文匹配，Agent 会被逼成「用户一提病名就换话题」。
 */
class AgentContentCheckerTest {

    private AppAgentProperties properties;
    private AgentContentChecker checker;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        checker = new AgentContentChecker(properties, faithfulnessChecker);
    }

    private AgentSourceCorpus corpusOf(String... userTexts) {
        return AgentSourceCorpus.ofTexts(
                List.of(userTexts), properties.getGuardrail().getFaithfulnessNgramSize());
    }

    // ---------- 诊断：只查新增区段 ----------

    @Test
    void shouldRejectDiagnosticStatementAddedByAgent() {
        AgentSourceCorpus corpus = corpusOf("我最近老是睡不着，白天也没精神");

        AgentGuardrailVerdict verdict = checker.check("你这是典型的焦虑症表现，建议你就医。", corpus);

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.DIAGNOSTIC);
    }

    /**
     * 用户自己用了病症词，Agent 共情复述时带上它——这是蓝图 §6.4 要求的恰当行为，
     * 不得判违规。
     */
    @Test
    void shouldNotFlagDiseaseTermWhenItComesFromUser() {
        AgentSourceCorpus corpus = corpusOf("我怀疑我可能有点焦虑症，但我不确定");

        AgentGuardrailVerdict verdict = checker.check("你说你可能有点焦虑症，那种感觉是什么样的？", corpus);

        assertThat(verdict.isPassed())
                .as("用户自己说出的病症词被 Agent 复述时不应误伤")
                .isTrue();
    }

    @Test
    void shouldRejectMedicalAdvice() {
        AgentSourceCorpus corpus = corpusOf("最近压力有点大");

        assertThat(checker.check("你需要吃药控制一下。", corpus).isPassed()).isFalse();
        assertThat(checker.check("建议就医，去看精神科。", corpus).isPassed()).isFalse();
    }

    @Test
    void shouldAcceptEmpatheticReply() {
        AgentSourceCorpus corpus = corpusOf("最近压力有点大，晚上总睡不着");

        assertThat(checker.check("听起来最近挺不容易的。", corpus).isPassed()).isTrue();
        assertThat(checker.check("这种感觉是从什么时候开始的？", corpus).isPassed()).isTrue();
        assertThat(checker.check("好的，这些已经很好了。", corpus).isPassed()).isTrue();
    }

    // ---------- 代决：谎报已执行不可逆操作 ----------

    /**
     * 补 C2 的盲区：白名单保证 Agent **做不到** seal，但拦不住它**说自己做了**。
     */
    @Test
    void shouldRejectFakeSealClaim() {
        AgentSourceCorpus corpus = corpusOf("帮我把这条封存吧");

        assertThat(checker.check("好，我已经帮你封存了。", corpus).isPassed()).isFalse();
        assertThat(checker.check("我把它封存了，你可以放心。", corpus).isPassed()).isFalse();
    }

    @Test
    void shouldRejectFakeDeleteAndUnlockClaims() {
        AgentSourceCorpus corpus = corpusOf("这条我不想要了");

        assertThat(checker.check("已经帮你删除了。", corpus).violation())
                .isEqualTo(AgentGuardrailViolation.FAKE_ACTION);
        assertThat(checker.check("已经帮你解锁了。", corpus).violation())
                .isEqualTo(AgentGuardrailViolation.FAKE_ACTION);
    }

    /**
     * 只是「建议」而非「声称已做」必须放行——否则 Agent 连正常建议都说不了。
     */
    @Test
    void shouldAcceptSuggestionWithoutClaimingExecution() {
        AgentSourceCorpus corpus = corpusOf("帮我把这条封存吧");

        assertThat(checker.check("封存要你自己在页面上确认，要不要现在去？", corpus).isPassed()).isTrue();
    }

    // ---------- 伪引用 ----------

    @Test
    void shouldRejectFabricatedQuote() {
        AgentSourceCorpus corpus = corpusOf("我学的是软件工程，一直想做后端");

        AgentGuardrailVerdict verdict = checker.checkQuotes(
                "你刚才说“我觉得这条路完全走错了，想彻底放弃”，是这样吗？", corpus);

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.FABRICATED_QUOTE);
    }

    @Test
    void shouldAcceptQuoteThatComesFromUser() {
        AgentSourceCorpus corpus = corpusOf("我学的是软件工程，一直想做后端");

        assertThat(checker.checkQuotes("你说“一直想做后端”，那现在还是这样想吗？", corpus).isPassed())
                .isTrue();
    }

    @Test
    void shouldAcceptTextWithoutQuotes() {
        AgentSourceCorpus corpus = corpusOf("我学的是软件工程");

        assertThat(checker.checkQuotes("要不要把这段放进正文？", corpus).isPassed()).isTrue();
    }

    // ---------- 开关与边界 ----------

    @Test
    void shouldPassWhenDisabledByConfig() {
        properties.getGuardrail().setContentCheckEnabled(false);

        assertThat(checker.check("你这是典型的焦虑症表现。", corpusOf("有点累")).isPassed()).isTrue();
    }

    @Test
    void shouldTreatBlankTextAsNothingToCheck() {
        AgentSourceCorpus corpus = corpusOf("有点累");

        assertThat(checker.check(null, corpus).isPassed()).isTrue();
        assertThat(checker.check("   ", corpus).isPassed()).isTrue();
    }

    /**
     * 规则匹配走归一化文本，所以标点变形不能绕过检查。
     */
    @Test
    void shouldNotBeBypassedByPunctuationInsertion() {
        AgentSourceCorpus corpus = corpusOf("最近有点累");

        assertThat(checker.check("你这是……典型的焦虑症！", corpus).isPassed()).isFalse();
    }
}
