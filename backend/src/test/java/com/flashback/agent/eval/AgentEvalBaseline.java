package com.flashback.agent.eval;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基线文件的读取与校验（C6，T-17 / T-18）。
 *
 * <h3>本类承载的三条纪律</h3>
 * <ol>
 * <li><b>基线必须有 baselineNote</b>：记录由哪一刀定、当时的 policyVersion。
 * 缺失即失败——一条没有出处的基线等于一个没人能复核的数字。</li>
 * <li><b>只改指标不改说明会被拦住</b>：checksum 由两者共同派生（见
 * {@link AgentEvalSnapshot#checksum()}）。这是防橡皮图章的机械落点。</li>
 * <li><b>不提供自动重写</b>：本类只读，没有任何写基线的方法，也不认
 * {@code -Dupdate} 之类的系统属性（N4 / design 决策 3）。</li>
 * </ol>
 */
final class AgentEvalBaseline {

    private static final String BASELINE_FILE = "eval/baseline/snapshots.yaml";

    private final Map<String, AgentEvalSnapshot> byCaseId;
    private final Map<String, String> checksums;

    private AgentEvalBaseline(Map<String, AgentEvalSnapshot> byCaseId, Map<String, String> checksums) {
        this.byCaseId = byCaseId;
        this.checksums = checksums;
    }

    @SuppressWarnings("unchecked")
    static AgentEvalBaseline load() {
        try (InputStream input = classLoader().getResourceAsStream(BASELINE_FILE)) {
            if (input == null) {
                throw new IllegalStateException(
                        "baseline file is missing: " + BASELINE_FILE
                                + " (it is the regression baseline; do not delete it)");
            }
            Object loaded;
            try {
                loaded = new Yaml().load(input);
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                        "failed to parse baseline file " + BASELINE_FILE
                                + " (" + ex.getClass().getSimpleName() + ")",
                        ex);
            }
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalStateException(
                        "baseline file " + BASELINE_FILE + " must be a mapping with a 'snapshots' key");
            }
            Object rawSnapshots = ((Map<String, Object>) root).get("snapshots");
            if (!(rawSnapshots instanceof List<?> list)) {
                throw new IllegalStateException(
                        "baseline file " + BASELINE_FILE + " must contain a 'snapshots' list");
            }

            Map<String, AgentEvalSnapshot> byCaseId = new LinkedHashMap<>();
            Map<String, String> checksums = new LinkedHashMap<>();
            for (Object element : list) {
                Map<String, Object> raw = (Map<String, Object>) element;
                AgentEvalSnapshot snapshot = AgentEvalSnapshot.fromBaseline(raw);
                Object checksum = raw.get("checksum");
                if (checksum == null || String.valueOf(checksum).isBlank()) {
                    throw new IllegalStateException(
                            "baseline entry for " + snapshot.caseId() + " has no checksum;"
                                    + " the checksum is what makes 'changed the numbers but not the note'"
                                    + " detectable");
                }
                if (byCaseId.put(snapshot.caseId(), snapshot) != null) {
                    throw new IllegalStateException("duplicate baseline caseId: " + snapshot.caseId());
                }
                checksums.put(snapshot.caseId(), String.valueOf(checksum).trim());
            }
            return new AgentEvalBaseline(byCaseId, checksums);

        } catch (IOException ex) {
            throw new IllegalStateException("failed to read baseline file " + BASELINE_FILE, ex);
        } catch (NoClassDefFoundError error) {
            throw new IllegalStateException(
                    "YAML parser unavailable; baseline cannot be read. Do NOT skip the comparison.", error);
        }
    }

    boolean has(String caseId) {
        return byCaseId.containsKey(caseId);
    }

    AgentEvalSnapshot get(String caseId) {
        return byCaseId.get(caseId);
    }

    /**
     * 基线文件里记录的 checksum（人写的），与 {@link AgentEvalSnapshot#checksum()}
     * （由指标 + 说明现场派生）比对。
     */
    String recordedChecksum(String caseId) {
        return checksums.get(caseId);
    }

    java.util.Set<String> caseIds() {
        return java.util.Set.copyOf(byCaseId.keySet());
    }

    static String baselineFile() {
        return BASELINE_FILE;
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loader == null ? AgentEvalBaseline.class.getClassLoader() : loader;
    }
}
