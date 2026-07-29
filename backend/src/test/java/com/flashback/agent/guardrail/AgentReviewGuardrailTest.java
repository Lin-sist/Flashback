package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回看场景下的护栏行为（C3b agent-review-chat）。
 *
 * 回看把被回看记录的正文放进 MEMORY 层（design 决策 4），因此**几乎每一轮都会触发
 * 时间归属判定**——这是全项目对该护栏压力最大的场景，也是 R8 未校准阈值第一次被真正施压。
 * 本类固定的是「压力下的行为方向」：宁可误伤成兜底回复，不放行把旧话冒充成此刻的话。
 */
class AgentReviewGuardrailTest {

    /** 被回看记录的正文，模拟三个月前写下的内容。 */
    private static final String RECORD_CONTENT = "那时候项目截止日期压得我喘不过气，不知道还能不能撑住";

    /** 用户在回看对话中此刻说的话。 */
    private static final String SAID_NOW = "现在回头看好像没那么严重了";

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

    /** 回看语料：SESSION 层是本次对话，MEMORY 层是被回看记录的内容。 */
    private AgentLayeredCorpus reviewCorpus() {
        return AgentLayeredCorpus.of(
                List.of(user(SAID_NOW)), List.of(RECORD_CONTENT),
                properties.getGuardrail().getFaithfulnessNgramSize());
    }

    // ---------- 记录内容进 MEMORY 层而非 SESSION 层 ----------

    @Test
    void reviewedRecordContentMustLiveInMemoryLayerNotSessionLayer() {
        AgentLayeredCorpus corpus = reviewCorpus();

        // 若记录正文进了 SESSION 层，Agent 复述它就不需要带时间归属，
        // 「你觉得撑不住」会读起来像用户此刻说的——正是 C3a 整层护栏要防的事。
        assertThat(AgentCoverageProfile.of(RECORD_CONTENT, corpus.sessionOnly()).coverage())
                .isLessThan(0.2d);
        assertThat(AgentCoverageProfile.of(RECORD_CONTENT, corpus.combined()).coverage())
                .isEqualTo(1.0d);
    }

    // ---------- 时间归属：回看的高频场景 ----------

    @Test
    void restatingRecordContentWithoutTimeAttributionMustBeDowngraded() {
        AgentGuardrailVerdict verdict = timeAttributionChecker.check(
                "项目截止日期压得你喘不过气，是这样吗", reviewCorpus());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION);
    }

    @Test
    void restatingRecordContentWithTimeAttributionMustPass() {
        List<String> replies = List.of(
                "那时候你写下项目截止日期压得你喘不过气，现在还会这样吗",
                "三月那会儿你说项目截止日期压得你喘不过气",
                "你当时写的是项目截止日期压得你喘不过气");

        for (String reply : replies) {
            assertThat(timeAttributionChecker.check(reply, reviewCorpus()).isPassed())
                    .as("回看里带时间归属的复述是正确行为，不得误伤：%s", reply)
                    .isTrue();
        }
    }

    @Test
    void respondingOnlyToWhatUserSaysNowNeedsNoAttribution() {
        assertThat(timeAttributionChecker.check(
                "听起来现在的你轻松了一些", reviewCorpus()).isPassed()).isTrue();
    }

    // ---------- 其余护栏在回看同样生效 ----------

    @Test
    void diagnosticCheckMustStillFireInReview() {
        AgentGuardrailVerdict verdict = contentChecker.check(
                "你这是典型的焦虑症表现，建议你就医", reviewCorpus().combined());

        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.DIAGNOSTIC);
    }

    @Test
    void fakeActionCheckMustStillFireInReview() {
        // 回看对象是已解锁记录，Agent 谎称帮用户删除尤其危险。
        AgentGuardrailVerdict verdict = contentChecker.check(
                "我已经帮你删除了这条记录", reviewCorpus().combined());

        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.FAKE_ACTION);
    }

    @Test
    void fabricatedQuoteMustStillBeRejectedInReview() {
        AgentGuardrailVerdict verdict = contentChecker.checkQuotes(
                "你那时写的是“我已经彻底放弃了”，对吗", reviewCorpus().combined());

        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.FABRICATED_QUOTE);
    }

    @Test
    void genuineRecordQuoteShouldPassInReview() {
        AgentGuardrailVerdict verdict = contentChecker.checkQuotes(
                "你那时写的是“项目截止日期压得我喘不过气”", reviewCorpus().combined());

        assertThat(verdict.isPassed()).isTrue();
    }

    // ---------- 阈值未被放宽 ----------

    @Test
    void reviewMustNotIntroduceLoosenedThresholds() {
        AppAgentProperties.Guardrail guardrail = properties.getGuardrail();

        assertThat(guardrail.getMinCoverage()).isEqualTo(0.35d);
        assertThat(guardrail.getMaxUncoveredRun()).isEqualTo(12);
        assertThat(guardrail.getMinCheckedLength()).isEqualTo(12);
        assertThat(guardrail.getMinMemoryOnlyRunForAttribution())
                .as("回看是这层护栏压力最大的场景，但阈值不得为它单独调松（design 决策 5）")
                .isEqualTo(8);
    }
}
