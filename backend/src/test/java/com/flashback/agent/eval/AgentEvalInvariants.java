package com.flashback.agent.eval;

import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.config.AppAgentProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 不变量断言（C6，T-09 ~ T-15）。
 *
 * <h3>这一层的失败语义：硬失败，不允许刷新</h3>
 * 本类断言的都是「变了就是 bug」的性质——阶段序列合法、同阶段追问不超上限、
 * 注入不超预算、回复不超长度上限、该降级必降级、无工具模式必 fail-closed。
 * 因此**没有**任何刷新、接受或跳过手段，也不提供
 * {@code -Dupdate} 之类的开关（N4 / design 决策 3）。
 *
 * 与快照层的分工：快照层回答「相对基线变了吗」（需人确认），
 * 本层回答「这还对吗」（不需要商量）。
 *
 * <h3>期望键必须被认识</h3>
 * 每条用例声明的 expect 键都要在这里被消费。写错键名（比如把
 * {@code downgradePath} 写成 {@code downgrade_path}）会**明确失败**，
 * 而不是静默跳过那条期望——后者会让用例看起来通过了，实际什么都没验。
 */
final class AgentEvalInvariants {

    /**
     * 本类认识的全部期望键。新增期望时必须同时在 {@link #verify} 里消费它。
     */
    private static final Set<String> KNOWN_KEYS = Set.of(
            "outcome",
            "violation",
            "downgradePath",
            "fallbackLocal",
            "providerCalls",
            "reflectionAttempted",
            "reflectionTerminal",
            "stagePath",
            "stageReasons",
            "stage",
            "stageReasonAbsent",
            "stageRetained",
            "maxReaskPerStage",
            "sessionEnded",
            "materialProduced",
            "memoryEnabled",
            "memoryFailed",
            "retrievedCount",
            "injectedCount",
            "injectionWithinBudget",
            "maxInjectedChars",
            "injectedCharsExactly",
            "memorySupplementPresent",
            "replyWithinLengthCap",
            "clipped",
            "toolRejectedReason",
            "proposedCount",
            "discardedCount");

    private AgentEvalInvariants() {
    }

    static void verify(AgentEvalRun run) {
        AgentEvalCase evalCase = run.evalCase();
        assertKnownExpectationKeys(evalCase);

        AgentTraceCollector trace = run.lastTrace();
        assertThat(trace)
                .as("用例 %s 未产生任何轨迹；可观测开启时每一轮都应有一条", evalCase.caseId())
                .isNotNull();

        // ---------- 通用不变量：无论用例声明什么，这些恒须成立 ----------
        verifyUniversalInvariants(run, trace);

        // ---------- 用例声明的期望 ----------
        if (evalCase.hasExpectation("outcome")) {
            assertThat(trace.outcome().name())
                    .as("用例 %s 的轮次结论", evalCase.caseId())
                    .isEqualTo(String.valueOf(evalCase.expected("outcome")));
        }
        if (evalCase.hasExpectation("violation")) {
            Object expected = evalCase.expected("violation");
            if (expected == null) {
                assertThat(trace.violation())
                        .as("用例 %s 不应产生违规", evalCase.caseId())
                        .isNull();
            } else {
                assertThat(trace.violation())
                        .as("用例 %s 的违规类型", evalCase.caseId())
                        .isEqualTo(String.valueOf(expected));
            }
        }
        if (evalCase.hasExpectation("downgradePath")) {
            Object expected = evalCase.expected("downgradePath");
            if (expected == null) {
                assertThat(trace.downgradePath())
                        .as("用例 %s 不应发生降级", evalCase.caseId())
                        .isNull();
            } else {
                assertThat(trace.downgradePath())
                        .as("用例 %s 的降级路径（哪一道闸拦下的）", evalCase.caseId())
                        .isEqualTo(String.valueOf(expected));
            }
        }
        if (evalCase.hasExpectation("fallbackLocal")) {
            // 兜底必须可与 provider 正常产出区分——沿用 C4 已接受的条款，不得回退。
            assertThat(run.stepValue(trace, "downgrade", "fallback"))
                    .as("用例 %s：兜底回复必须标明来自本地，不得伪装成模型正常输出", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.boolOf(evalCase.expected("fallbackLocal")) ? "local" : "none");
        }
        if (evalCase.hasExpectation("providerCalls")) {
            assertThat(run.harness().client().replyCallCount())
                    .as("用例 %s 的 reply provider 调用数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("providerCalls")));
        }
        if (evalCase.hasExpectation("reflectionAttempted")) {
            assertThat(run.stepValue(trace, "reflection-result", "attempted"))
                    .as("用例 %s 是否实际进入 reflection", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.boolOf(evalCase.expected("reflectionAttempted")));
        }
        if (evalCase.hasExpectation("reflectionTerminal")) {
            assertThat(run.stepValue(trace, "reflection-result", "terminal"))
                    .as("用例 %s 的 reflection 终态", evalCase.caseId())
                    .isEqualTo(String.valueOf(evalCase.expected("reflectionTerminal")));
        }

        verifyStageExpectations(run, trace, evalCase);
        verifyMemoryExpectations(run, trace, evalCase);
        verifyLengthExpectations(run, trace, evalCase);
        verifyToolExpectations(run, trace, evalCase);
    }

    /**
     * 与用例声明无关的通用不变量。
     *
     * 这些不写在 YAML 里，因为它们不是「这条用例想验的东西」，
     * 而是**每条用例都顺便验一遍**的底线。放在这里的收益是：
     * 将来任何一条新用例都自动受它们保护，不需要作者记得声明。
     */
    private static void verifyUniversalInvariants(AgentEvalRun run, AgentTraceCollector trace) {
        AppAgentProperties properties = run.harness().properties();
        String caseId = run.evalCase().caseId();

        // 1. 回复长度硬上限在任何叠加之后仍然生效。
        assertThat(run.lastReplyLength())
                .as("用例 %s：回复长度不得超过 maxReplyChars", caseId)
                .isLessThanOrEqualTo(properties.getMaxReplyChars());

        // 2. 同阶段追问不得超过代码常量上限。
        assertThat(run.maxConsecutiveReask())
                .as("用例 %s：同阶段追问不得超过 MAX_REASK_PER_STAGE=1（不逼问）", caseId)
                .isLessThanOrEqualTo(com.flashback.agent.AgentStageMachine.MAX_REASK_PER_STAGE);

        // 3. 注入规模不得超过派生上限（E25：无聚合预算配置项，上限是派生的）。
        for (AgentTraceCollector each : run.traces()) {
            Object injectedCount = run.stepValue(each, "memory-injected", "injectedCount");
            Object injectedChars = run.stepValue(each, "memory-injected", "injectedChars");
            if (injectedCount != null) {
                assertThat(AgentEvalCase.intOf(injectedCount))
                        .as("用例 %s：注入片段数不得超过 memory.maxFragments", caseId)
                        .isLessThanOrEqualTo(properties.getMemory().getMaxFragments());
            }
            if (injectedChars != null) {
                int derivedCap = properties.getMemory().getMaxFragments()
                        * properties.getMemory().getMaxFragmentChars();
                assertThat(AgentEvalCase.intOf(injectedChars))
                        .as("用例 %s：注入总长度不得超过派生上限 maxFragments*maxFragmentChars=%d"
                                + "（该上限是派生值，项目中没有聚合字符预算配置项）", caseId, derivedCap)
                        .isLessThanOrEqualTo(derivedCap);
            }
        }

        // 4. 阶段判定结论只能取自既有枚举，不得出现并行语义。
        for (String reason : run.stageReasons()) {
            assertThat(reason)
                    .as("用例 %s：阶段判定结论必须取自 AgentStageDecision.Reason", caseId)
                    .isIn("ADVANCE", "REASK", "USER_FINISH_INTENT", "TURN_LIMIT_REACHED", "CLOSED");
        }

        // 5. 一轮一条：轮次序号必须严格不减，且不得凭空跳号。
        int previousTurn = 0;
        for (AgentTraceCollector each : run.traces()) {
            assertThat(each.turnNo())
                    .as("用例 %s：轨迹的轮次序号不得回退", caseId)
                    .isGreaterThanOrEqualTo(previousTurn);
            previousTurn = each.turnNo();
        }

        // 6. 版本锚点必须在位——C6 的回归比对依赖它按版本分组。
        assertThat(trace.promptVersion())
                .as("用例 %s：提示词版本锚点缺失，回归比对将无法按版本分组", caseId)
                .isNotNull();
        assertThat(trace.policyVersion())
                .as("用例 %s：护栏规则版本锚点缺失", caseId)
                .isNotNull();
    }

    private static void verifyStageExpectations(
            AgentEvalRun run, AgentTraceCollector trace, AgentEvalCase evalCase) {

        if (evalCase.hasExpectation("stagePath")) {
            assertThat(run.stagePath())
                    .as("用例 %s 的阶段推进路径", evalCase.caseId())
                    .isEqualTo(stringsOf(evalCase.expected("stagePath")));
        }
        if (evalCase.hasExpectation("stageReasons")) {
            assertThat(run.stageReasons())
                    .as("用例 %s 的阶段判定结论序列", evalCase.caseId())
                    .isEqualTo(stringsOf(evalCase.expected("stageReasons")));
        }
        if (evalCase.hasExpectation("stage")) {
            assertThat(trace.stage().name())
                    .as("用例 %s 的最终阶段", evalCase.caseId())
                    .isEqualTo(String.valueOf(evalCase.expected("stage")));
        }
        if (evalCase.hasExpectation("stageReasonAbsent")
                && AgentEvalCase.boolOf(evalCase.expected("stageReasonAbsent"))) {
            assertThat(trace.stageReason())
                    .as("用例 %s：无阶段机的模式不得伪造一个判定结论", evalCase.caseId())
                    .isNull();
        }
        if (evalCase.hasExpectation("stageRetained")
                && AgentEvalCase.boolOf(evalCase.expected("stageRetained"))) {
            assertThat(run.hasStep(trace, "stage-retained"))
                    .as("用例 %s：应记 stage-retained 而非 stage-decision", evalCase.caseId())
                    .isTrue();
        }
        if (evalCase.hasExpectation("maxReaskPerStage")) {
            assertThat(run.maxConsecutiveReask())
                    .as("用例 %s 的同阶段最大连续追问次数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("maxReaskPerStage")));
        }
        if (evalCase.hasExpectation("sessionEnded")) {
            assertThat(run.stepValue(trace, "session-ended", "reason"))
                    .as("用例 %s 的会话收束原因", evalCase.caseId())
                    .isEqualTo(String.valueOf(evalCase.expected("sessionEnded")));
        }
        if (evalCase.hasExpectation("materialProduced")) {
            boolean expected = AgentEvalCase.boolOf(evalCase.expected("materialProduced"));
            Object produced = run.stepValue(trace, "material", "produced");
            if (expected) {
                assertThat(produced)
                        .as("用例 %s：应产出素材", evalCase.caseId())
                        .isEqualTo(true);
            } else {
                assertThat(produced)
                        .as("用例 %s：不应产出素材", evalCase.caseId())
                        .isNotEqualTo(true);
            }
        }
    }

    private static void verifyMemoryExpectations(
            AgentEvalRun run, AgentTraceCollector trace, AgentEvalCase evalCase) {

        if (evalCase.hasExpectation("memoryEnabled")) {
            assertThat(run.stepValue(trace, "memory-retrieval", "enabled"))
                    .as("用例 %s：记忆开关状态必须可读，且与「无命中」可区分", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.boolOf(evalCase.expected("memoryEnabled")));
        }
        if (evalCase.hasExpectation("memoryFailed")) {
            assertThat(run.stepValue(trace, "memory-retrieval", "failed"))
                    .as("用例 %s：检索失败状态必须与「无命中」可区分", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.boolOf(evalCase.expected("memoryFailed")));
        }
        if (evalCase.hasExpectation("retrievedCount")) {
            Object retrieved = run.stepValue(trace, "memory-retrieval", "retrievedCount");
            int actual = retrieved == null ? 0 : AgentEvalCase.intOf(retrieved);
            assertThat(actual)
                    .as("用例 %s 的检索命中数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("retrievedCount")));
        }
        if (evalCase.hasExpectation("injectedCount")) {
            Object injected = run.stepValue(trace, "memory-injected", "injectedCount");
            int actual = injected == null ? 0 : AgentEvalCase.intOf(injected);
            assertThat(actual)
                    .as("用例 %s 的实际注入片段数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("injectedCount")));
        }
        if (evalCase.hasExpectation("injectionWithinBudget")
                && AgentEvalCase.boolOf(evalCase.expected("injectionWithinBudget"))) {
            // 通用不变量已覆盖，这里保留显式声明的能力，使用例可读出「这条在验预算」。
            AppAgentProperties.Memory memory = run.harness().properties().getMemory();
            Object injectedChars = run.stepValue(trace, "memory-injected", "injectedChars");
            if (injectedChars != null) {
                assertThat(AgentEvalCase.intOf(injectedChars))
                        .as("用例 %s：注入总长度须在派生上限内", evalCase.caseId())
                        .isLessThanOrEqualTo(memory.getMaxFragments() * memory.getMaxFragmentChars());
            }
        }
        if (evalCase.hasExpectation("maxInjectedChars")) {
            Object injectedChars = run.stepValue(trace, "memory-injected", "injectedChars");
            assertThat(injectedChars == null ? 0 : AgentEvalCase.intOf(injectedChars))
                    .as("用例 %s：单条片段须被截断到 maxFragmentChars 以内", evalCase.caseId())
                    .isLessThanOrEqualTo(AgentEvalCase.intOf(evalCase.expected("maxInjectedChars")));
        }
        if (evalCase.hasExpectation("injectedCharsExactly")) {
            // 「不超过上限」对一条本来就短的片段是废话——它恒成立而什么都没验。
            // 要证明截断真的发生了，必须断言长度**恰好等于**上限。
            Object injectedChars = run.stepValue(trace, "memory-injected", "injectedChars");
            assertThat(injectedChars == null ? 0 : AgentEvalCase.intOf(injectedChars))
                    .as("用例 %s：片段长度须恰好等于截断上限，以证明截断确实发生", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("injectedCharsExactly")));
        }
        if (evalCase.hasExpectation("memorySupplementPresent")) {
            assertThat(run.stepValue(trace, "prompt", "memorySupplement"))
                    .as("用例 %s：上下文是否含记忆补充段", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.boolOf(evalCase.expected("memorySupplementPresent")));
        }
    }

    private static void verifyLengthExpectations(
            AgentEvalRun run, AgentTraceCollector trace, AgentEvalCase evalCase) {

        if (evalCase.hasExpectation("replyWithinLengthCap")
                && AgentEvalCase.boolOf(evalCase.expected("replyWithinLengthCap"))) {
            assertThat(run.lastReplyLength())
                    .as("用例 %s：回复长度须在硬上限内", evalCase.caseId())
                    .isLessThanOrEqualTo(run.harness().properties().getMaxReplyChars());
        }
        if (evalCase.hasExpectation("clipped")) {
            boolean expected = AgentEvalCase.boolOf(evalCase.expected("clipped"));
            assertThat(run.hasStep(trace, "reply-clipped"))
                    .as("用例 %s：回复是否被长度上限裁剪", evalCase.caseId())
                    .isEqualTo(expected);
            if (expected) {
                // 裁剪不算降级——内容仍是 provider 的产出，只是被截短。
                assertThat(trace.outcome().name())
                        .as("用例 %s：裁剪不得把轮次结论改成降级", evalCase.caseId())
                        .isNotEqualTo("DOWNGRADED");
                Object before = run.stepValue(trace, "reply-clipped", "beforeLength");
                Object after = run.stepValue(trace, "reply-clipped", "afterLength");
                assertThat(AgentEvalCase.intOf(after))
                        .as("用例 %s：裁剪后长度必须小于裁剪前", evalCase.caseId())
                        .isLessThan(AgentEvalCase.intOf(before));
            }
        }
    }

    private static void verifyToolExpectations(
            AgentEvalRun run, AgentTraceCollector trace, AgentEvalCase evalCase) {

        if (evalCase.hasExpectation("toolRejectedReason")) {
            assertThat(run.stepValue(trace, "tool-rejected", "reason"))
                    .as("用例 %s 的提议被拒原因（须取自 AgentToolValidationResult 的结构化常量）",
                            evalCase.caseId())
                    .isEqualTo(String.valueOf(evalCase.expected("toolRejectedReason")));
        }
        if (evalCase.hasExpectation("proposedCount")) {
            Object proposed = run.stepValue(trace, "tools", "proposedCount");
            assertThat(proposed == null ? 0 : AgentEvalCase.intOf(proposed))
                    .as("用例 %s 的待确认提议数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("proposedCount")));
        }
        if (evalCase.hasExpectation("discardedCount")) {
            Object discarded = run.stepValue(trace, "tools-fail-closed", "discardedCount");
            assertThat(discarded == null ? 0 : AgentEvalCase.intOf(discarded))
                    .as("用例 %s：无工具模式下被 fail-closed 丢弃的提议数", evalCase.caseId())
                    .isEqualTo(AgentEvalCase.intOf(evalCase.expected("discardedCount")));
        }
    }

    /**
     * 校验期望键都被认识。
     *
     * 这一条守的是「静默失效」：拼错的键在旧实现里会被忽略，
     * 于是用例文件里写着一条期望，实际什么都没验，而测试是绿的。
     */
    private static void assertKnownExpectationKeys(AgentEvalCase evalCase) {
        List<String> unknown = new ArrayList<>();
        for (String key : evalCase.expectationKeys()) {
            if (!KNOWN_KEYS.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            fail("用例 " + evalCase.caseId() + "（" + evalCase.source() + "）声明了未被消费的期望键 "
                    + unknown + "；拼错的期望键会被静默忽略，因此这里必须失败");
        }
    }

    private static List<String> stringsOf(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object element : list) {
            result.add(String.valueOf(element));
        }
        return List.copyOf(result);
    }
}
