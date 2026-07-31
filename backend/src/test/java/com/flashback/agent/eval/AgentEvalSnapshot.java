package com.flashback.agent.eval;

import com.flashback.agent.trace.AgentTraceCollector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一条用例的快照指标（C6，T-16 ~ T-18）。
 *
 * <h3>快照层与不变量层的区别</h3>
 * 不变量层回答「这还对吗」——变了就是 bug，硬失败。
 * 快照层回答「相对基线变了吗」——变化可能完全合理（比如故意改了引导话术），
 * 因此失败语义是**需人确认**，而不是自动判错。
 *
 * <h3>快照里没有一个字是文本</h3>
 * 全部字段是枚举短标识、计数、长度与比例。这不是靠自觉：快照派生自
 * {@link AgentTraceCollector}，而 C5 已在类型层把「任意文本进轨迹」堵死
 * （{@code step(...)} 是私有可变参数入口）。本类只是继承了那个性质。
 *
 * <h3>为什么带 checksum（design 决策 4，本刀防橡皮图章的落点）</h3>
 * 快照框架最常见的死法是沦为橡皮图章：红了就把数字改成当前值，绿灯拿到，
 * 而「为什么变了」从此无人知晓。本项目已经明确拒绝过一次「靠后人自觉维持边界」
 * 的设计（蓝图 §9.6），理由同样适用于此。
 *
 * 所以 checksum 由**指标 + baselineNote 共同派生**：只改数字不改说明，
 * checksum 就对不上。手法与 C5 拒绝手工 bump 版本号同源——
 * 把「人必须记得同步」变成「不同步就报错」。
 */
final class AgentEvalSnapshot {

    private final String caseId;
    private final Map<String, Object> metrics;
    private final String baselineNote;

    private AgentEvalSnapshot(String caseId, Map<String, Object> metrics, String baselineNote) {
        this.caseId = caseId;
        this.metrics = metrics;
        this.baselineNote = baselineNote;
    }

    /**
     * 从一次运行派生快照。
     *
     * 字段选择的依据是「改动会让它变、而它变了值得看一眼」：
     * 阶段路径与判定序列反映编排决策；provider 调用次数是反思环（C7）
     * 最直接的观测点；注入规模反映上下文预算；降级层反映护栏；
     * 长度与长度比反映克制。
     */
    static AgentEvalSnapshot of(AgentEvalRun run, String baselineNote) {
        AgentTraceCollector trace = run.lastTrace();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("turns", run.traces().size());
        metrics.put("stagePath", run.stagePath());
        metrics.put("stageReasons", run.stageReasons());
        metrics.put("outcome", trace == null ? null : trace.outcome().name());
        metrics.put("providerCalls", run.harness().client().replyCallCount());
        metrics.put("materialCalls", run.harness().client().materialCallCount());
        metrics.put("injectedCount", intOrZero(run.stepValue(trace, "memory-injected", "injectedCount")));
        metrics.put("injectedChars", intOrZero(run.stepValue(trace, "memory-injected", "injectedChars")));
        metrics.put("promptMessageCount", intOrZero(run.stepValue(trace, "prompt", "messageCount")));
        metrics.put("downgradeLayer", trace == null ? null : trace.downgradePath());
        metrics.put("violation", trace == null ? null : trace.violation());
        metrics.put("replyLength", run.lastReplyLength());
        metrics.put("replyToInputRatio", run.replyToInputRatio());
        return new AgentEvalSnapshot(run.evalCase().caseId(), metrics, baselineNote);
    }

    /**
     * 从基线文件里的一条记录还原。
     */
    @SuppressWarnings("unchecked")
    static AgentEvalSnapshot fromBaseline(Map<String, Object> raw) {
        String caseId = String.valueOf(raw.get("caseId"));
        Object note = raw.get("baselineNote");
        if (note == null || String.valueOf(note).isBlank()) {
            throw new IllegalStateException(
                    "baseline entry for " + caseId + " has no baselineNote;"
                            + " every baseline must record which change set it and at which policyVersion");
        }
        Object metrics = raw.get("metrics");
        if (!(metrics instanceof Map<?, ?> metricsMap)) {
            throw new IllegalStateException("baseline entry for " + caseId + " has no metrics");
        }
        return new AgentEvalSnapshot(
                caseId, new LinkedHashMap<>((Map<String, Object>) metricsMap), String.valueOf(note).trim());
    }

    String caseId() {
        return caseId;
    }

    Map<String, Object> metrics() {
        return Map.copyOf(metrics);
    }

    String baselineNote() {
        return baselineNote;
    }

    /**
     * 由「指标 + baselineNote」共同派生的校验值。
     *
     * 两者都进指纹是本机制的全部要点：只把指标纳入，改数字不改说明就检测不到；
     * 只把说明纳入，改数字就检测不到。理由与 C5 的 policyVersion
     * 同时纳入 prompt 条款与检查词表一致——它们是同一份声明的两面。
     */
    String checksum() {
        return fingerprint(canonicalMetrics() + "\n@note=" + baselineNote);
    }

    /**
     * 指标的规范化字符串形式，供比对与指纹使用。
     */
    String canonicalMetrics() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            builder.append(entry.getKey()).append('=').append(render(entry.getValue())).append('\n');
        }
        return builder.toString().trim();
    }

    /**
     * 指标是否与另一份一致（不含 baselineNote）。
     */
    boolean metricsMatch(AgentEvalSnapshot other) {
        return canonicalMetrics().equals(other.canonicalMetrics());
    }

    /**
     * 供人粘贴进基线文件的 YAML 片段。
     *
     * 刻意提供它：手工更新是**故意保留的摩擦**（不提供自动重写开关），
     * 但摩擦不该是「自己去把十个数字抄对」。人需要做的判断是
     * 「这个变化对不对」以及「在 baselineNote 里写清为什么」。
     */
    String toYamlBlock() {
        StringBuilder builder = new StringBuilder();
        builder.append("  - caseId: ").append(caseId).append('\n');
        builder.append("    baselineNote: \"<在此写明：由哪一刀定基线、为什么变化>\"\n");
        builder.append("    metrics:\n");
        for (Map.Entry<String, Object> entry : metrics.entrySet()) {
            builder.append("      ").append(entry.getKey()).append(": ")
                    .append(renderYaml(entry.getValue())).append('\n');
        }
        builder.append("    checksum: \"<写完 baselineNote 后跑一次，用报错信息里的实际值填这里>\"\n");
        return builder.toString();
    }

    private static Object intOrZero(Object value) {
        return value == null ? 0 : AgentEvalCase.intOf(value);
    }

    private static String render(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof java.util.List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(String.valueOf(list.get(i)));
            }
            return builder.append(']').toString();
        }
        return String.valueOf(value);
    }

    private static String renderYaml(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof java.util.List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(String.valueOf(list.get(i)));
            }
            return builder.append(']').toString();
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return String.valueOf(value);
    }

    private static String fingerprint(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(String.format("%02x", hashed[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
