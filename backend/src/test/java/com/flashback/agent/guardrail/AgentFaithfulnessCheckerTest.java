package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 忠实度判定核心测试（C4）。
 *
 * 本类是 C4 的核心验收证据：
 * - R1 真实样本回归（验收标准 2）
 * - 整理不误伤（验收标准 3）
 * - **双指标必要性**（验收标准 4）——证明仅覆盖率拦不住 R1
 */
class AgentFaithfulnessCheckerTest {

    /**
     * R1 实测的用户原话（C2 闸门 3，ACTIVE_TASK R1）。
     */
    private static final String R1_USER_TURN_1 = "我学的是软件工程，一直想做后端";
    private static final String R1_USER_TURN_2 = "刚才说的这些我觉得挺重要的，想留下来";

    /**
     * R1 实测中模型增写的句子——**用户从未说过**。
     */
    private static final String R1_FABRICATED = "但最近心里有点空，不知道该不该继续沿着这条路走下去，方向是不是对的，自己也说不清楚";

    private AppAgentProperties properties;
    private AgentFaithfulnessChecker checker;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        checker = new AgentFaithfulnessChecker(properties);
    }

    private AgentSourceCorpus r1Corpus() {
        return corpusOf(R1_USER_TURN_1, R1_USER_TURN_2);
    }

    private AgentSourceCorpus corpusOf(String... userMessages) {
        List<AgentMessage> history = new ArrayList<>();
        for (String content : userMessages) {
            history.add(message(AgentMessageRole.USER, content));
        }
        return AgentSourceCorpus.of(history, properties.getGuardrail().getFaithfulnessNgramSize());
    }

    private AgentMessage message(AgentMessageRole role, String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    // ---------- R1 回归（验收标准 2） ----------

    @Test
    void shouldRejectR1RealSampleThatAppendsFabricatedSentence() {
        // R1 的真实形态：两句真话 + 一句用户从未说过的话。
        String candidate = R1_USER_TURN_1 + "。" + R1_FABRICATED + "。" + R1_USER_TURN_2 + "。";

        AgentGuardrailVerdict verdict = checker.check(candidate, r1Corpus());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.UNFAITHFUL);
        assertThat(verdict.reason()).isEqualTo("unfaithful");
    }

    @Test
    void shouldRejectFabricatedSentenceAlone() {
        AgentGuardrailVerdict verdict = checker.check(R1_FABRICATED, r1Corpus());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.UNFAITHFUL);
    }

    // ---------- 双指标必要性（验收标准 4） ----------

    /**
     * 这条测试是「为什么必须两个指标」的证据。
     *
     * R1 样本在**仅覆盖率**判据下会通过——虚构句只占整体的一部分，
     * 整体覆盖率仍高于阈值；只有加上「最长连续未覆盖片段」才能拦住它。
     * 若未来有人以「覆盖率够用」为由删掉第二个指标，本测试会失败。
     */
    @Test
    void coverageAloneMustNotBeSufficientToCatchR1() {
        String candidate = R1_USER_TURN_1 + "。" + R1_FABRICATED + "。" + R1_USER_TURN_2 + "。";
        AgentSourceCorpus corpus = r1Corpus();

        // 只放开连续未覆盖片段判据（设为极大），保留默认覆盖率阈值。
        properties.getGuardrail().setMaxUncoveredRun(Integer.MAX_VALUE);
        AgentGuardrailVerdict coverageOnly = checker.check(candidate, corpus);

        assertThat(coverageOnly.isPassed())
                .as("仅靠整体覆盖率无法拦住 R1 型增写，故第二个指标是必要的")
                .isTrue();
        assertThat(coverageOnly.coverage()).isGreaterThan(properties.getGuardrail().getMinCoverage());

        // 恢复默认后，最长连续未覆盖片段判据命中。
        properties.getGuardrail().setMaxUncoveredRun(12);
        AgentGuardrailVerdict bothIndicators = checker.check(candidate, corpus);

        assertThat(bothIndicators.isPassed()).isFalse();
        assertThat(bothIndicators.maxUncoveredRun()).isGreaterThan(12);
    }

    // ---------- 整理不误伤（验收标准 3） ----------

    @Test
    void shouldAcceptReorderedSentences() {
        // 顺语序：把两句调换顺序，未新增任何内容。
        String candidate = R1_USER_TURN_2 + "。" + R1_USER_TURN_1 + "。";

        assertThat(checker.check(candidate, r1Corpus()).isPassed()).isTrue();
    }

    @Test
    void shouldAcceptPunctuationAndWhitespaceChanges() {
        String candidate = "我学的是软件工程  一直想做后端！！！刚才说的这些我觉得挺重要的……想留下来";

        assertThat(checker.check(candidate, r1Corpus()).isPassed()).isTrue();
    }

    @Test
    void shouldAcceptRemovalOfFillerWords() {
        AgentSourceCorpus corpus = corpusOf("嗯，那个，我最近就是那种睡不好，然后白天也没精神");
        // 去掉口头语后的整理结果。
        String candidate = "我最近睡不好，白天也没精神";

        assertThat(checker.check(candidate, corpus).isPassed()).isTrue();
    }

    @Test
    void shouldAcceptConcatenationOfMultipleUserMessages() {
        AgentSourceCorpus corpus = corpusOf(
                "工作上有点撑不住", "主要是项目排期太紧", "我想把这种状态记下来");
        String candidate = "工作上有点撑不住，主要是项目排期太紧，我想把这种状态记下来";

        assertThat(checker.check(candidate, corpus).isPassed()).isTrue();
    }

    @Test
    void shouldAcceptShortConnectorInsertionAtSeams() {
        AgentSourceCorpus corpus = corpusOf("今天面试没过", "有点难受");
        // 接缝处插入短连接词，未覆盖片段远小于阈值。
        String candidate = "今天面试没过，所以有点难受";

        AgentGuardrailVerdict verdict = checker.check(candidate, corpus);

        assertThat(verdict.isPassed()).isTrue();
        assertThat(verdict.maxUncoveredRun()).isLessThanOrEqualTo(12);
    }

    // ---------- 边界与语义 ----------

    @Test
    void shouldRejectWhenUserSaidNothing() {
        // 用户一句话都没说却产出会进正文的文本，不可能忠实。
        AgentGuardrailVerdict verdict = checker.check("你今天看起来状态不错", corpusOf());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.UNFAITHFUL);
    }

    @Test
    void shouldNotUseAssistantMessagesAsSource() {
        // Agent 自己说过的话不能成为「合法来源」，否则忠实度闸自我失效。
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.ASSISTANT, "听起来你最近为方向的事情很纠结，心里有点空"),
                message(AgentMessageRole.USER, "嗯"));
        AgentSourceCorpus corpus = AgentSourceCorpus.of(
                history, properties.getGuardrail().getFaithfulnessNgramSize());

        AgentGuardrailVerdict verdict = checker.check("最近为方向的事情很纠结，心里有点空", corpus);

        assertThat(verdict.isPassed()).isFalse();
    }

    @Test
    void shouldSkipCoverageCheckForShortTextButKeepRunCheck() {
        AgentSourceCorpus corpus = corpusOf("今天有点累");

        // 短文本：低于 minCheckedLength，覆盖率判据不生效。
        properties.getGuardrail().setMinCheckedLength(50);
        assertThat(checker.check("今天累", corpus).isPassed()).isTrue();

        // 但连续未覆盖片段判据仍然生效。
        properties.getGuardrail().setMaxUncoveredRun(3);
        assertThat(checker.check("完全无关的另一段陈述内容", corpus).isPassed()).isFalse();
    }

    @Test
    void shouldTreatBlankCandidateAsNothingToCheck() {
        assertThat(checker.check(null, r1Corpus()).isPassed()).isTrue();
        assertThat(checker.check("   ", r1Corpus()).isPassed()).isTrue();
    }

    @Test
    void shouldPassWithTraceWhenDisabledByConfig() {
        properties.getGuardrail().setFaithfulnessEnabled(false);

        // 开关关闭时放行，但实现侧会记录结构化日志说明判定未生效。
        assertThat(checker.check(R1_FABRICATED, r1Corpus()).isPassed()).isTrue();
    }

    /**
     * fail-closed：判定过程自身异常时必须判违规，绝不放行未检文本。
     * 这是护栏最关键的失败方向——检查坏了不能等于检查通过。
     */
    @Test
    void shouldFailClosedWhenCheckThrows() {
        AgentFaithfulnessChecker failing = new AgentFaithfulnessChecker(properties) {
            @Override
            public AgentCoverageProfile profileOf(String candidate, AgentSourceCorpus corpus) {
                throw new IllegalStateException("boom");
            }
        };

        AgentGuardrailVerdict verdict = failing.check("任何内容", r1Corpus());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.CHECK_ERROR);
    }

    @Test
    void shouldExposeStructuredMetricsWithoutContent() {
        AgentGuardrailVerdict verdict = checker.check(R1_FABRICATED, r1Corpus());

        String metrics = verdict.metrics();

        assertThat(metrics).contains("coverage=", "maxUncoveredRun=", "checkedLength=");
        // 痕迹里不得出现任何候选文本或用户原话片段。
        assertThat(metrics).doesNotContain("软件工程", "心里有点空", "想留下来");
    }
}
