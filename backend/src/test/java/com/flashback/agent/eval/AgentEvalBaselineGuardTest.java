package com.flashback.agent.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防橡皮图章机制的自验（C6，T-18 / 验收 19）。
 *
 * <h3>为什么这个机制自己必须被测</h3>
 * 快照框架最常见的死法是沦为橡皮图章：红了就把数字改成当前值，绿灯拿到，
 * 而「为什么变了」从此无人知晓。C6 用「checksum 由指标 + baselineNote 共同派生」
 * 来拦它——但**如果这个拦截本身失效了，没有任何信号会告诉你**。
 *
 * 本项目已经明确拒绝过一次「靠后人自觉维持边界」的设计（蓝图 §9.6），
 * 拒绝的理由正是「无测试报警」。同一条逻辑要求本机制配一个直接验证：
 * 构造「只改数字不改说明」这个确切情形，断言它**被拦住**。
 *
 * 这是本刀防橡皮图章的落点，也是它与「在文档里写一句要求」的区别。
 */
@DisplayName("C6 基线防橡皮图章机制")
class AgentEvalBaselineGuardTest {

    /**
     * 只改指标数值、不改 baselineNote → checksum 必须对不上。
     *
     * 这就是橡皮图章的确切形态：把红的数字改成当前值让它变绿。
     */
    @Test
    void changingMetricsWithoutUpdatingTheNoteMustChangeTheChecksum() {
        String note = "C6 定基线（2026-07-31）：某条用例";
        AgentEvalSnapshot original = snapshot(note, 1, 0);
        AgentEvalSnapshot tampered = snapshot(note, 2, 0);

        assertThat(tampered.checksum())
                .as("只改指标不改说明时 checksum 必须变化，否则橡皮图章无法被检测")
                .isNotEqualTo(original.checksum());
    }

    /**
     * 只改 baselineNote、不改指标 → checksum 也必须变。
     *
     * 这个方向同样重要，但理由不同：它保证 checksum 真的把说明纳入了指纹。
     * 若只把指标纳入，那么「改数字 + 顺手改说明」这个**正确**流程算出来的
     * checksum 就与「只改数字」相同，于是上一条测试会通过，而机制其实是残缺的。
     *
     * 手法与 C5 的 policyVersion 同时纳入 prompt 条款与检查词表一致——
     * 它们是同一份声明的两面，只纳入一面就等于给自己留一个观测盲区。
     */
    @Test
    void changingOnlyTheNoteMustAlsoChangeTheChecksum() {
        AgentEvalSnapshot original = snapshot("C6 定基线：原因 A", 1, 0);
        AgentEvalSnapshot renoted = snapshot("C6 定基线：原因 B", 1, 0);

        assertThat(renoted.checksum())
                .as("说明必须参与指纹，否则「改数字+改说明」与「只改数字」算出同一个值")
                .isNotEqualTo(original.checksum());
    }

    /**
     * 指标与说明都不变 → checksum 稳定。
     *
     * 若它不稳定，评测每次跑都会红，整套机制会在两天内被人关掉。
     */
    @Test
    void identicalMetricsAndNoteMustProduceAStableChecksum() {
        String note = "C6 定基线（2026-07-31）：某条用例";
        assertThat(snapshot(note, 3, 42).checksum())
                .isEqualTo(snapshot(note, 3, 42).checksum());
    }

    /**
     * 缺 baselineNote 的基线条目必须被拒绝。
     *
     * 一条没有出处的基线等于一个没人能复核的数字——它看起来在守着什么，
     * 实际上没人知道它守的是不是对的。
     */
    @Test
    void baselineEntryWithoutNoteMustBeRejected() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("caseId", "some-case");
        raw.put("metrics", Map.of("turns", 1));

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> AgentEvalSnapshot.fromBaseline(raw)).getMessage())
                .contains("baselineNote");
    }

    /**
     * 不提供自动重写基线的手段（N4 / design 决策 3）。
     *
     * 用反射检查是刻意的：这条约束的实质是「将来没有人会顺手加一个 -Dupdate」。
     * 写在文档里的禁令挡不住一个赶时间的下午，一条测试可以。
     *
     * 之所以连方法名都查：自动重写只要存在，无论怎么命名，
     * 「评测红了」的最短路径就会变成跑一下它，而那正是本机制要防的事。
     */
    @Test
    void noAutomaticBaselineRewriteMustExist() {
        List<Class<?>> evalClasses = List.of(
                AgentEvalBaseline.class,
                AgentEvalSnapshot.class,
                AgentEvalCaseLoader.class,
                AgentEvalInvariants.class);

        for (Class<?> type : evalClasses) {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                String name = method.getName().toLowerCase(java.util.Locale.ROOT);
                assertThat(name)
                        .as("%s.%s 看起来像一个自动重写基线的入口；"
                                + "手工更新是刻意保留的摩擦（design 决策 3）",
                                type.getSimpleName(), method.getName())
                        .doesNotContain("write")
                        .doesNotContain("save")
                        .doesNotContain("update")
                        .doesNotContain("rewrite")
                        .doesNotContain("accept")
                        .doesNotContain("approve");
            }
        }
    }

    /**
     * 基线更新提示片段里不得含用例输入文本。
     *
     * 失败输出也是一种产物：它会被贴进终端、issue、聊天窗口。
     * 隐私边界对它同样适用。
     */
    @Test
    void snapshotYamlBlockMustNotContainCaseText() {
        AgentEvalSnapshot snapshot = snapshot("C6 定基线", 1, 0);
        String block = snapshot.toYamlBlock();

        assertThat(block)
                .as("可粘贴片段只应含字段名与数值")
                .doesNotContain("我")
                .doesNotContain("你")
                .doesNotContain("焦虑");
    }

    /**
     * 构造一份指标形状与真实快照一致的假快照。
     *
     * 刻意不通过 {@link AgentEvalRun} 造：本类要验的是 checksum 机制本身，
     * 不该依赖跑一遍编排——那会让这几条测试的失败原因变得含糊。
     */
    private static AgentEvalSnapshot snapshot(String note, int turns, int injectedChars) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("turns", turns);
        metrics.put("stagePath", List.of("CONFUSION"));
        metrics.put("stageReasons", List.of("ADVANCE"));
        metrics.put("outcome", "SUCCESS");
        metrics.put("providerCalls", 1);
        metrics.put("materialCalls", 0);
        metrics.put("injectedCount", 0);
        metrics.put("injectedChars", injectedChars);
        metrics.put("promptMessageCount", 4);
        metrics.put("downgradeLayer", null);
        metrics.put("violation", null);
        metrics.put("replyLength", 14);
        metrics.put("replyToInputRatio", 0.78);

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("caseId", "guard-fixture");
        raw.put("baselineNote", note);
        raw.put("metrics", metrics);
        return AgentEvalSnapshot.fromBaseline(raw);
    }
}
