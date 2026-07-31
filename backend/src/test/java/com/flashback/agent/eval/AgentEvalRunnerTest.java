package com.flashback.agent.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 评测运行器（C6，T-07）。
 *
 * <h3>为什么它就是一个普通 JUnit 测试（N6 / design 决策 11）</h3>
 * 它离线、零外调、毫秒级，没有任何理由默认跳过。做成 profile 或环境变量门控
 * 会让它变成「需要有人记得去跑」的资产，而本刀存在的全部意义就是不再依赖有人记得。
 *
 * <h3>诚实边界（必须一起读）</h3>
 * <ul>
 * <li>本仓库<b>没有 CI</b>（无 {@code .github/}，workflow 零命中）。因此架构宪法 §3.6
 * 写的「CI 可跑子集」当前**没有落点**——本刀交付的是「一条 maven 命令可跑」，
 * <b>不是</b> CI 门槛。</li>
 * <li>评测跑在确定性替身上，评的是**编排逻辑**，不是模型的语言质量。
 * 语言质量靠真实探针的小样本人评锚定，而那份锚点当前为空（见
 * {@code eval/baseline/narrative-anchors.yaml}）。没有假装用 Judge 覆盖它。</li>
 * <li>快照指标在**真实 provider** 下的稳定性未验证（本刀 0 外调）。</li>
 * </ul>
 */
@DisplayName("C6 Agent 评测")
class AgentEvalRunnerTest {

    static List<AgentEvalCase> cases() {
        return AgentEvalCaseLoader.loadAll();
    }

    /**
     * 不变量层：硬失败，无刷新手段。
     */
    @ParameterizedTest(name = "[不变量] {0}")
    @MethodSource("cases")
    void invariantsMustHold(AgentEvalCase evalCase) {
        AgentEvalInvariants.verify(AgentEvalRun.execute(evalCase));
    }

    /**
     * 快照层：失败 = 需人确认，不是自动判错。
     *
     * 失败时打印「基线 vs 当前」与可粘贴的新片段，但**不自动写回**——
     * 更新基线必须保持为一个需要人判断的动作，且要在 baselineNote 里写清原因。
     */
    @ParameterizedTest(name = "[快照] {0}")
    @MethodSource("cases")
    void snapshotsMustMatchBaseline(AgentEvalCase evalCase) {
        AgentEvalBaseline baseline = AgentEvalBaseline.load();
        AgentEvalRun run = AgentEvalRun.execute(evalCase);

        if (!baseline.has(evalCase.caseId())) {
            AgentEvalSnapshot current = AgentEvalSnapshot.of(run, "<待填>");
            fail("""
                    用例 %s 尚无基线。

                    这不是自动补基线的时机——请人工确认下面的指标是否是你期望的样子，
                    然后把它粘进 %s，并在 baselineNote 里写明由哪一刀定的基线。

                    %s""".formatted(evalCase.caseId(), AgentEvalBaseline.baselineFile(),
                    current.toYamlBlock()));
        }

        AgentEvalSnapshot recorded = baseline.get(evalCase.caseId());
        AgentEvalSnapshot current = AgentEvalSnapshot.of(run, recorded.baselineNote());

        // 先验基线自身没被橡皮图章式改动过（改了数字没改说明）。
        String recordedChecksum = baseline.recordedChecksum(evalCase.caseId());
        if (!recorded.checksum().equals(recordedChecksum)) {
            fail("""
                    基线条目 %s 的 checksum 对不上。

                    checksum 由「指标 + baselineNote」共同派生，因此这通常意味着：
                    有人改了基线里的指标数值，却没有同步更新 baselineNote 说明为什么改。
                    那正是快照沦为橡皮图章的形态，所以这里必须失败。

                    请更新 baselineNote 说明本次变更的原因，然后把 checksum 改为：%s
                    """.formatted(evalCase.caseId(), recorded.checksum()));
        }

        if (!current.metricsMatch(recorded)) {
            fail("""
                    用例 %s 的快照指标相对基线发生了变化。

                    这**不一定是缺陷**——如果你刚改了引导话术、阈值或编排逻辑，变化可能完全合理。
                    但它需要人看一眼：确认变化符合预期后，手工更新基线，
                    并在 baselineNote 里写明是哪一刀改的、为什么。

                    没有自动刷新开关，这是刻意的（design 决策 3）。

                    --- 基线 ---
                    %s

                    --- 当前 ---
                    %s

                    --- 可粘贴的新片段（记得写 baselineNote）---
                    %s""".formatted(
                    evalCase.caseId(),
                    recorded.canonicalMetrics(),
                    current.canonicalMetrics(),
                    current.toYamlBlock()));
        }
    }

    /**
     * 基线不得残留已删除用例的条目。
     *
     * 存在理由：孤儿基线会让「这条用例还在被守着」变成假象——
     * 用例删了、基线留着，从文件上看不出这个维度已经没人验了。
     */
    @Test
    void baselineMustNotContainOrphanEntries() {
        List<String> caseIds = new ArrayList<>();
        for (AgentEvalCase evalCase : cases()) {
            caseIds.add(evalCase.caseId());
        }
        AgentEvalBaseline baseline = AgentEvalBaseline.load();
        List<String> orphans = new ArrayList<>();
        for (String baselineCaseId : baseline.caseIds()) {
            if (!caseIds.contains(baselineCaseId)) {
                orphans.add(baselineCaseId);
            }
        }
        assertThat(orphans)
                .as("基线里有已不存在的用例条目；请一并删除，否则会看起来仍有回归在守着它")
                .isEmpty();
    }

    /**
     * 本地真实样本缺失时静默跳过，不影响入库用例。
     *
     * 这条不是「测一个 if」——它固定了一个协作契约：别人 clone 仓库时本地样本
     * 必然不存在（它按设计不入库），此时评测必须照常全绿。
     */
    @Test
    void localSamplesMustBeOptional() {
        List<AgentEvalCase> loaded = AgentEvalCaseLoader.loadOptional(AgentEvalCaseLoader.localSampleFile());
        if (AgentEvalCaseLoader.localSamplesPresent()) {
            assertThat(loaded)
                    .as("本地样本文件存在时，它的用例应被纳入")
                    .isNotEmpty();
        } else {
            assertThat(loaded)
                    .as("本地样本文件不存在时静默跳过，不得失败——它按设计不入库")
                    .isEmpty();
        }
    }

    /**
     * 入库用例缺失必须硬失败。
     *
     * 与上一条相反的语义：合成用例是回归基线，缺了就是资产被误删。
     */
    @Test
    void requiredCaseFileMustFailWhenMissing() {
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        IllegalStateException.class,
                        () -> AgentEvalCaseLoader.loadRequired("eval/cases/does-not-exist.yaml"))
                        .getMessage())
                .as("入库用例文件缺失时必须明确失败，不得静默跳过")
                .contains("missing");
    }
}
