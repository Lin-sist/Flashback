package com.flashback.agent.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 话术质量维度的结构自检（C6，T-21 / 验收 15）。
 *
 * <h3>这个维度当前是什么状态</h3>
 * **结构就位，锚点为空。** 这是 N7 定稿的形态（design 决策 8）：
 * 填充锚点需要真实 provider 产出 + 人工评判，属闸门 3 范围，而本刀外调预算为 0。
 *
 * <h3>为什么空结构也要测</h3>
 * 空文件最大的风险是被误读成「这个维度已经覆盖了」。本类因此做两件事：
 * 一是固定文件的形状（将来填的时候有约束可循），
 * 二是**要求文件里显式写着它为空以及为什么**——一旦有人把那段说明删掉、
 * 或者悄悄把锚点填成占位数据，测试会失败。
 *
 * 这与 {@link AgentEvalBaselineGuardTest} 是同一条思路：靠机制而不是靠自觉。
 */
@DisplayName("C6 话术质量锚点结构")
class AgentEvalNarrativeAnchorTest {

    private static final String ANCHOR_FILE = "eval/baseline/narrative-anchors.yaml";

    /**
     * 锚点文件必须存在且形状正确。
     */
    @Test
    @SuppressWarnings("unchecked")
    void anchorFileMustExistWithTheExpectedShape() throws IOException {
        Object loaded = new Yaml().load(read());
        assertThat(loaded)
                .as("锚点文件必须是带 anchors 键的映射")
                .isInstanceOf(Map.class);
        assertThat(((Map<String, Object>) loaded).get("anchors"))
                .as("anchors 必须是列表（当前为空列表）")
                .isInstanceOf(List.class);
    }

    /**
     * 锚点为空时，文件必须显式说明它为空、为什么空、何时填。
     *
     * 这一条守的不是格式，而是**诚实性**：一个空文件加一句「TODO」
     * 与一个空文件加三条不做 Judge 的理由，对半年后的读者是完全不同的东西。
     */
    @Test
    @SuppressWarnings("unchecked")
    void emptyAnchorsMustBeAccompaniedByAnHonestExplanation() throws IOException {
        String content = read();
        List<?> anchors = (List<?>) ((Map<String, Object>) new Yaml().load(content)).get("anchors");

        if (anchors.isEmpty()) {
            assertThat(content)
                    .as("锚点为空时必须写明「空不等于已覆盖」，否则空文件会被误读成该维度已完成")
                    .contains("空 ≠ 该维度已覆盖");
            assertThat(content)
                    .as("必须写明不做 LLM-as-Judge 的理由（D31）")
                    .contains("隐私")
                    .contains("预算")
                    .contains("不可复现");
            assertThat(content)
                    .as("必须写明建议的填充时机，否则分不清是「决定不做」还是「忘了做」")
                    .contains("C7");
        }
    }

    /**
     * 锚点文件不得含回复原文或日记原文。
     *
     * 它入库，因此隐私等级与快照相同。锚点的设计是「某版本在某阶段的语气评级」，
     * 不是语料库——reasonTag 用受控词表正是为了避免有人顺手粘一句原话进来。
     */
    @Test
    @SuppressWarnings("unchecked")
    void filledAnchorsMustNotCarryReplyText() throws IOException {
        List<?> anchors = (List<?>) ((Map<String, Object>) new Yaml().load(read())).get("anchors");

        for (Object element : anchors) {
            Map<String, Object> anchor = (Map<String, Object>) element;
            assertThat(anchor.keySet())
                    .as("锚点只允许结构化字段，不得出现承载文本的键")
                    .doesNotContain("reply", "text", "content", "quote", "excerpt");
            assertThat(anchor)
                    .as("锚点必须记录评判时的版本锚点，否则无法与轨迹对齐")
                    .containsKeys("promptVersion", "policyVersion", "rating");
        }
    }

    private static String read() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = loader.getResourceAsStream(ANCHOR_FILE)) {
            assertThat(input).as("锚点文件缺失：%s", ANCHOR_FILE).isNotNull();
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
