package com.flashback.agent.eval;

import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 评测产物的隐私边界（C6，T-19 / T-20、验收 21–22）。
 *
 * <h3>与既有那条隐私断言的关系（design 决策 12）</h3>
 * C4 的 {@code AgentGuardrailBoundaryCaseTest.verdictMetricsMustNotLeakContent}
 * **原地保留、断言零修改**。但它是**负向子串断言**（列举四个词说「不许出现」），
 * 只能守住当初那几个词：换一批用例文本，或某天实现里不小心把别的文本塞进指标串，
 * 它一声不响就放过去了。
 *
 * 本类改用**白名单式结构校验**：断言指标串整体符合纯数值形状。
 * 那是「只允许这些」而不是「不允许那些」，强一个量级。
 * 两条并存不是重复——旧的是 C4 的历史资产（证明当时确实检查过这件事），
 * 新的是 C6 的守护。删旧的会丢历史，改旧的会破纪律。
 */
@DisplayName("C6 评测隐私边界")
class AgentEvalPrivacyTest {

    /**
     * 用于检测泄漏的特征串。
     *
     * 取一个绝不会出现在任何规则词表、枚举或字段名里的短语——
     * 若它出现在任何产物中，只可能是从用例输入原样漏出去的。
     * 沿用 C5 {@code SECRET_MARKER} 的做法。
     */
    private static final String MARKER = "紫罗兰色的旧铁皮盒子";

    /**
     * 判定指标串的合法形状：只有键名与数值，一个非数字的内容都不许有。
     */
    private static final Pattern METRICS_SHAPE = Pattern.compile(
            "coverage=\\d+\\.\\d{3} maxUncoveredRun=\\d+ checkedLength=\\d+");

    /**
     * 结构化校验：指标串必须整体匹配纯数值形状。
     *
     * 这是本刀相对 C4 那条断言的加强点。它不关心「有没有出现某个词」，
     * 而是要求整串**只能**长成这个样子——任何文本混进来都无处藏身。
     */
    @Test
    void verdictMetricsMustMatchANumericOnlyShape() {
        AppAgentProperties properties = new AppAgentProperties();
        AgentFaithfulnessChecker checker = new AgentFaithfulnessChecker(properties);
        AgentSourceCorpus corpus = AgentSourceCorpus.ofTexts(
                List.of(MARKER + "，我把这件事记在这里"),
                properties.getGuardrail().getFaithfulnessNgramSize());

        AgentGuardrailVerdict verdict = checker.check(
                MARKER + "，还有一些我从来没说过的话被硬加了进来", corpus);

        assertThat(verdict.metrics())
                .as("指标串必须整体符合纯数值形状；白名单式校验强于逐词黑名单")
                .matches(METRICS_SHAPE);
    }

    /**
     * 跑一轮把特征串同时埋进用户输入与记忆片段，断言轨迹里没有它。
     *
     * 这条与 C5 的隐私断言方向相同，但落点不同：C5 验的是**落库的轨迹**，
     * 本条验的是**评测这条链路**——评测会把收集器读出来派生快照，
     * 多了一层搬运，也就多了一个漏点。
     */
    @Test
    void traceReadByEvalMustNotContainCaseText() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .recordContent(MARKER + "，当时写下这些的时候心里很乱")
                .memoryCandidate(70001L, MARKER + "，那阵子一直没睡好",
                        java.time.LocalDateTime.of(2026, 3, 14, 21, 0))
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply("这种感觉是从什么时候开始的？"));

        harness.turn(MARKER + "，最近又想起这件事了");

        AgentTraceCollector trace = harness.sink().last();
        assertThat(renderTrace(trace))
                .as("轨迹只含结构化标识、数值与长度，绝不含任何输入文本")
                .doesNotContain(MARKER);
    }

    /**
     * 快照的规范化形式不得含任何输入文本。
     *
     * 快照会入库，是本刀产物里隐私等级最高的一份。
     */
    @Test
    void snapshotMustNotContainCaseText() {
        AgentEvalHarness harness = AgentEvalHarness.builder()
                .recordContent(MARKER)
                .memoryCandidate(70001L, MARKER + "，那阵子一直没睡好",
                        java.time.LocalDateTime.of(2026, 3, 14, 21, 0))
                .build();
        harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply("这种感觉是从什么时候开始的？"));
        harness.turn(MARKER + "，最近又想起这件事了");

        // 走真实的派生路径（同一条 runner 用的那条），验的是派生过程不漏文本。
        AgentEvalRun run = AgentEvalRun.execute(privacyProbeCase());
        AgentEvalSnapshot snapshot = AgentEvalSnapshot.of(run, "隐私自检夹具");

        assertThat(snapshot.canonicalMetrics()).doesNotContain(MARKER);
        assertThat(snapshot.toYamlBlock()).doesNotContain(MARKER);
    }

    /**
     * 一条只存在于内存的用例：把特征串埋进用户输入，用于验证派生链路不漏文本。
     *
     * 刻意不放进入库的用例文件——它的输入是给泄漏检测用的噪声，
     * 混进正式用例集会让那批资产的意图变糊。
     */
    private static AgentEvalCase privacyProbeCase() {
        return AgentEvalCase.from(Map.of(
                "caseId", "privacy-probe-in-memory",
                "dimension", "GUARDRAIL",
                "turns", List.of(Map.of(
                        "userInput", MARKER + "，最近又想起这件事了",
                        "reply", "这种感觉是从什么时候开始的？")),
                "expect", Map.of("outcome", "SUCCESS")), "in-memory://privacy-probe");
    }

    /**
     * 入库的快照基线文件本身不得含任何自然语言用例输入。
     *
     * 判据是结构性的：除注释与 baselineNote 之外，值只能是枚举短标识、数字、
     * 列表与 null。这样它不依赖某个特征串，换一批用例照样成立。
     */
    @Test
    void baselineFileMustNotContainFreeformCaseText() throws IOException {
        String content = readResource("eval/baseline/snapshots.yaml");

        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")
                    || trimmed.startsWith("- caseId:")
                    || trimmed.startsWith("baselineNote:")
                    || trimmed.startsWith("checksum:")
                    || trimmed.equals("metrics:")
                    || trimmed.startsWith("snapshots:")) {
                // baselineNote 是刻意允许自然语言的字段——它承载的是「为什么定这个基线」，
                // 由人书写、面向人阅读。它的约束是「不得写入用户日记原文」，
                // 这一点由 code review 与本文件顶部的说明共同保证，
                // 而不是靠机械规则（机械规则区分不了「我写的解释」与「用户写的日记」）。
                continue;
            }
            assertThat(trimmed)
                    .as("基线的指标行只允许键名与结构化值；出现自然语言即可能是用例文本泄漏：%s", trimmed)
                    .matches("[a-zA-Z]+: (null|-?\\d+(\\.\\d+)?|\"[A-Z_a-z\\-]*\"|\\[[A-Z_,\\s]*\\])");
        }
    }

    /**
     * 入库的用例文件不得含真实凭证形态的内容。
     *
     * 用例是合成的，但它们是入库文件，顺手守一道：不该出现看起来像 key/token 的东西。
     */
    @Test
    void caseFilesMustNotContainCredentialLookingValues() throws IOException {
        for (String path : List.of(
                "eval/cases/stage-progression.yaml",
                "eval/cases/restraint.yaml",
                "eval/cases/memory.yaml",
                "eval/cases/guardrail.yaml")) {
            String content = readResource(path).toLowerCase(java.util.Locale.ROOT);
            assertThat(content)
                    .as("%s 不得含凭证形态内容", path)
                    .doesNotContain("api_key")
                    .doesNotContain("apikey")
                    .doesNotContain("secret")
                    .doesNotContain("password")
                    .doesNotContain("bearer ");
        }
    }

    /**
     * 把轨迹渲染成一个字符串，用于扫描泄漏。
     *
     * 覆盖全部字段与全部步骤——只扫 steps 会漏掉 causeType、model 这些顶层字段。
     */
    private static String renderTrace(AgentTraceCollector trace) {
        StringBuilder builder = new StringBuilder();
        builder.append(trace.traceId()).append('|')
                .append(trace.sessionId()).append('|')
                .append(trace.userId()).append('|')
                .append(trace.recordId()).append('|')
                .append(trace.turnNo()).append('|')
                .append(trace.attemptNo()).append('|')
                .append(trace.purpose()).append('|')
                .append(trace.stage()).append('|')
                .append(trace.stageReason()).append('|')
                .append(trace.model()).append('|')
                .append(trace.promptVersion()).append('|')
                .append(trace.policyVersion()).append('|')
                .append(trace.outcome()).append('|')
                .append(trace.providerDurationMs()).append('|')
                .append(trace.causeType()).append('|')
                .append(trace.downgradePath()).append('|')
                .append(trace.violation()).append('\n');
        for (Map<String, Object> step : trace.steps()) {
            for (Map.Entry<String, Object> entry : step.entrySet()) {
                builder.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static String readResource(String path) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = loader.getResourceAsStream(path)) {
            assertThat(input).as("资源缺失：%s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
