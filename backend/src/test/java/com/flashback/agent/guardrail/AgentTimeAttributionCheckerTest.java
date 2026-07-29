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
 * 时间归属判定测试（C3 agent-memory-retrieval）。
 *
 * 核心验收：Agent 复述历史原话时必须说清那是过去哪个时候的事。
 * 误伤方向的正例数量刻意多于负例——把正常回忆句判违规的代价是
 * 用户收到兜底回复、Agent 显得突然失忆，比放过一句生硬表达更糟。
 */
class AgentTimeAttributionCheckerTest {

    private static final String SESSION_SAID = "今天开会又被临时加了需求，有点烦";
    private static final String MEMORY_SAID = "三月那次项目截止日期压得我喘不过气";

    private AppAgentProperties properties;
    private AgentTimeAttributionChecker checker;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        checker = new AgentTimeAttributionChecker(properties);
    }

    private AgentMessage user(String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(AgentMessageRole.USER);
        message.setContent(content);
        return message;
    }

    private AgentLayeredCorpus corpus(List<String> fragments, String... sessionMessages) {
        List<AgentMessage> history = new ArrayList<>();
        for (String content : sessionMessages) {
            history.add(user(content));
        }
        return AgentLayeredCorpus.of(
                history, fragments, properties.getGuardrail().getFaithfulnessNgramSize());
    }

    private AgentLayeredCorpus withMemory() {
        return corpus(List.of(MEMORY_SAID), SESSION_SAID);
    }

    // ---------- 违规方向 ----------

    @Test
    void shouldRejectMemoryRestatementWithoutTimeAttribution() {
        // 裸复述：读起来像用户刚刚说的，实际是过去写下的。
        AgentGuardrailVerdict verdict = checker.check("项目截止日期压得你喘不过气，这次也是这样吗", withMemory());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION);
        assertThat(verdict.reason()).isEqualTo("missing-time-attribution");
    }

    @Test
    void violationShouldReportMemoryOnlyRunLength() {
        AgentGuardrailVerdict verdict = checker.check("项目截止日期压得你喘不过气", withMemory());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.maxUncoveredRun())
                .as("痕迹里要能读出复述了多长的旧内容")
                .isGreaterThanOrEqualTo(properties.getGuardrail().getMinMemoryOnlyRunForAttribution());
    }

    // ---------- 不误伤方向 ----------

    @Test
    void shouldAcceptMemoryRestatementWithExplicitMonth() {
        assertThat(checker.check(
                "我记得三月那次项目截止日期压得你喘不过气，这次也是同一件事吗", withMemory()).isPassed())
                .isTrue();
    }

    @Test
    void shouldAcceptVariousTimeAttributionPhrasings() {
        List<String> replies = List.of(
                "以前你也写过项目截止日期压得你喘不过气",
                "那时候你说项目截止日期压得你喘不过气",
                "之前你提过项目截止日期压得你喘不过气",
                "去年你写下项目截止日期压得你喘不过气",
                "上次你说过项目截止日期压得你喘不过气",
                "几个月前你写过项目截止日期压得你喘不过气");

        for (String reply : replies) {
            assertThat(checker.check(reply, withMemory()).isPassed())
                    .as("合法的时间归属表述不得被误伤：%s", reply)
                    .isTrue();
        }
    }

    @Test
    void shouldAcceptReplyThatOnlyUsesSessionContent() {
        // Agent 只回应此刻的话，没有复述记忆 → 无需时间归属。
        assertThat(checker.check("今天被临时加需求确实挺烦的，你想多说点吗", withMemory()).isPassed())
                .isTrue();
    }

    @Test
    void shouldAcceptShortIncidentalMemoryOverlap() {
        // 用户此刻与过去用了同一个短语，属措辞巧合，不要求时间归属。
        AgentLayeredCorpus corpus = corpus(List.of("那时候压力有点大"), "最近压力有点大");

        assertThat(checker.check("压力有点大的时候，先别急着要答案", corpus).isPassed()).isTrue();
    }

    @Test
    void shouldAcceptAgentOwnQuestionWithoutMemory() {
        assertThat(checker.check("今天是什么让你想写下这一刻", withMemory()).isPassed()).isTrue();
    }

    // ---------- 无记忆层时等价于 C4 现状 ----------

    @Test
    void shouldPassWhenNoMemoryLayer() {
        AgentLayeredCorpus sessionOnly = corpus(List.of(), SESSION_SAID);

        // 即便这句话与任何来源都无关，本检查也不管——它只负责时间归属这一件事，
        // 其余仍由 C4 的忠实度与内容检查负责。
        assertThat(checker.check("项目截止日期压得你喘不过气", sessionOnly).isPassed()).isTrue();
    }

    @Test
    void shouldPassForNullOrBlankInputs() {
        assertThat(checker.check(null, withMemory()).isPassed()).isTrue();
        assertThat(checker.check("   ", withMemory()).isPassed()).isTrue();
        assertThat(checker.check("任何内容", null).isPassed()).isTrue();
    }

    // ---------- 开关与 fail-closed ----------

    @Test
    void shouldPassWhenContentCheckDisabled() {
        properties.getGuardrail().setContentCheckEnabled(false);

        assertThat(checker.check("项目截止日期压得你喘不过气", withMemory()).isPassed()).isTrue();
    }

    /**
     * fail-closed：判定过程自身异常时必须判违规。
     * 检查坏了不能等于检查通过——这是护栏最关键的失败方向。
     */
    @Test
    void shouldFailClosedWhenCheckThrows() {
        AgentTimeAttributionChecker failing = new AgentTimeAttributionChecker(properties) {
            @Override
            int memoryOnlyRunOf(String text, AgentLayeredCorpus corpus) {
                throw new IllegalStateException("boom");
            }
        };

        AgentGuardrailVerdict verdict = failing.check("任何内容", withMemory());

        assertThat(verdict.isPassed()).isFalse();
        assertThat(verdict.violation()).isEqualTo(AgentGuardrailViolation.CHECK_ERROR);
    }

    @Test
    void thresholdShouldBeConfigurable() {
        // 阈值放大到不可能触发 → 同一句话放行；说明阈值确实生效且可配。
        properties.getGuardrail().setMinMemoryOnlyRunForAttribution(Integer.MAX_VALUE);

        assertThat(checker.check("项目截止日期压得你喘不过气", withMemory()).isPassed()).isTrue();
    }

    @Test
    void traceMustNotLeakContent() {
        AgentGuardrailVerdict verdict = checker.check("项目截止日期压得你喘不过气", withMemory());

        assertThat(verdict.metrics()).doesNotContain("项目截止日期", "喘不过气");
    }
}
