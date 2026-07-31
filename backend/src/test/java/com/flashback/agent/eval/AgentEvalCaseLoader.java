package com.flashback.agent.eval;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用例加载（C6，T-06 / T-08）。
 *
 * <h3>为什么用 snakeyaml 而不是新增依赖（N1 / design 决策 6）</h3>
 * snakeyaml 2.2 已在测试 classpath 上（经 {@code spring-boot-starter} 传递引入），
 * 因此零 pom 改动。选 YAML 而非 JSON 是因为用例文件要装中文、多行文本与解释性注释——
 * JSON 不支持注释。
 *
 * <h3>它是传递依赖，所以必须硬失败（决策 6 的强制缓解项）</h3>
 * snakeyaml 不是本项目直接声明的依赖，将来某次 starter 升级理论上可能移除它
 * （Boot 4.x 是代际重置，这不是纯理论担忧）。如果那天到来而本类**静默跳过**用例，
 * 评测就会变成最坏的形态：**绿灯，但什么都没测**。
 * 所以解析不可用、文件缺失、用例为空这三种情况全部明确失败。
 *
 * <h3>入库样例 vs 本地真实样本</h3>
 * 两者走同一个 runner，失败语义相反：
 * <ul>
 * <li>入库合成用例缺失 → <b>硬失败</b>（它是回归基线，缺了就是资产被误删）；</li>
 * <li>本地真实样本缺失 → <b>静默跳过</b>（它按设计不入库，别人 clone 下来本就没有）。</li>
 * </ul>
 */
final class AgentEvalCaseLoader {

    /** 入库的合成用例文件。缺任何一个都算资产损坏。 */
    private static final List<String> REQUIRED_CASE_FILES = List.of(
            "eval/cases/stage-progression.yaml",
            "eval/cases/restraint.yaml",
            "eval/cases/memory.yaml",
            "eval/cases/guardrail.yaml");

    /**
     * 本地真实样本。**已 gitignore**（`.gitignore` 的 `*.local.yaml` 通配规则）。
     *
     * 命名用 `.local.yaml` 而非蓝图原文的 `local-samples.yaml`：C5 已经因为
     * 「点名单个文件」吃过一次教训——新增的本地文件不会被规则覆盖，
     * 而这里漏出去的是用户日记原文。通配让规则对未来的样本文件同样成立。
     */
    private static final String LOCAL_SAMPLE_FILE = "eval/samples.local.yaml";

    private AgentEvalCaseLoader() {
    }

    /**
     * 加载全部用例：入库合成样例 + 本地真实样本（若存在）。
     */
    static List<AgentEvalCase> loadAll() {
        List<AgentEvalCase> cases = new ArrayList<>();
        for (String path : REQUIRED_CASE_FILES) {
            cases.addAll(loadRequired(path));
        }
        cases.addAll(loadOptional(LOCAL_SAMPLE_FILE));

        if (cases.isEmpty()) {
            throw new IllegalStateException("eval case set is empty; the asset is broken");
        }
        assertUniqueCaseIds(cases);
        return List.copyOf(cases);
    }

    /**
     * 入库用例：缺失即失败。
     */
    static List<AgentEvalCase> loadRequired(String resourcePath) {
        List<AgentEvalCase> cases = read(resourcePath, true);
        if (cases.isEmpty()) {
            throw new IllegalStateException("required eval case file has no cases: " + resourcePath);
        }
        return cases;
    }

    /**
     * 本地样本：缺失时静默跳过。
     */
    static List<AgentEvalCase> loadOptional(String resourcePath) {
        return read(resourcePath, false);
    }

    static boolean localSamplesPresent() {
        return resourceExists(LOCAL_SAMPLE_FILE);
    }

    static String localSampleFile() {
        return LOCAL_SAMPLE_FILE;
    }

    private static boolean resourceExists(String resourcePath) {
        try (InputStream input = classLoader().getResourceAsStream(resourcePath)) {
            return input != null;
        } catch (IOException ex) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<AgentEvalCase> read(String resourcePath, boolean required) {
        try (InputStream input = classLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                if (required) {
                    throw new IllegalStateException(
                            "required eval case file is missing: " + resourcePath
                                    + " (it is a regression baseline; do not delete it)");
                }
                return List.of();
            }

            Object loaded;
            try {
                loaded = new Yaml().load(input);
            } catch (RuntimeException ex) {
                // 明确失败而不是跳过：一个格式错误的用例文件必须停下来，
                // 否则「少跑了几条用例」会表现为一次通过。
                throw new IllegalStateException(
                        "failed to parse eval case file " + resourcePath
                                + " (" + ex.getClass().getSimpleName() + ")",
                        ex);
            }
            if (loaded == null) {
                return List.of();
            }
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalStateException(
                        "eval case file " + resourcePath + " must be a mapping with a 'cases' key");
            }
            Object rawCases = ((Map<String, Object>) root).get("cases");
            if (!(rawCases instanceof List<?> list)) {
                throw new IllegalStateException(
                        "eval case file " + resourcePath + " must contain a 'cases' list");
            }

            List<AgentEvalCase> cases = new ArrayList<>();
            for (Object element : list) {
                if (!(element instanceof Map<?, ?> caseMap)) {
                    throw new IllegalStateException(
                            "eval case file " + resourcePath + " contains a malformed case entry");
                }
                cases.add(AgentEvalCase.from((Map<String, Object>) caseMap, resourcePath));
            }
            return List.copyOf(cases);

        } catch (IOException ex) {
            throw new IllegalStateException("failed to read eval case file " + resourcePath, ex);
        } catch (NoClassDefFoundError error) {
            // 传递依赖消失时的兜底。落到这里说明 snakeyaml 不在 classpath 上了——
            // 必须明确失败（design 决策 6），绝不能表现为「全部用例通过」。
            throw new IllegalStateException(
                    "YAML parser unavailable on the test classpath; eval cannot run."
                            + " snakeyaml arrives transitively via spring-boot-starter."
                            + " Declare it explicitly or migrate the case format — do NOT skip the cases.",
                    error);
        }
    }

    private static void assertUniqueCaseIds(List<AgentEvalCase> cases) {
        Set<String> seen = new HashSet<>();
        for (AgentEvalCase evalCase : cases) {
            if (!seen.add(evalCase.caseId())) {
                // caseId 是快照对齐的键。重复会让两条用例共用一条基线，
                // 于是其中一条的回归静默失效。
                throw new IllegalStateException("duplicate eval caseId: " + evalCase.caseId());
            }
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? AgentEvalCaseLoader.class.getClassLoader() : loader;
    }
}
