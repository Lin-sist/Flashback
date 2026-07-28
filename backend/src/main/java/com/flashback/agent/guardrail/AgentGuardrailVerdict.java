package com.flashback.agent.guardrail;

/**
 * 护栏判定结果（C4）。
 *
 * 隐私（design.md §6）：本记录只承载**结构化指标**——覆盖比例、最长连续未覆盖片段长度、
 * 受检字符数与违规类型。**不得**携带候选文本、用户原话或未覆盖片段的内容，
 * 因为本记录会被写入审计与结构化日志。
 *
 * @param violation       违规类型；null 表示通过
 * @param coverage        候选文本被来源覆盖的比例（0~1）
 * @param maxUncoveredRun 最长连续未覆盖字符数
 * @param checkedLength   归一化后的受检字符数
 */
public record AgentGuardrailVerdict(
        AgentGuardrailViolation violation,
        double coverage,
        int maxUncoveredRun,
        int checkedLength) {

    private static final AgentGuardrailVerdict PASS = new AgentGuardrailVerdict(null, 1.0d, 0, 0);

    /**
     * 无需判定或判定通过（无指标场景，如空文本、开关关闭）。
     */
    public static AgentGuardrailVerdict pass() {
        return PASS;
    }

    public static AgentGuardrailVerdict pass(double coverage, int maxUncoveredRun, int checkedLength) {
        return new AgentGuardrailVerdict(null, coverage, maxUncoveredRun, checkedLength);
    }

    public static AgentGuardrailVerdict violation(
            AgentGuardrailViolation violation, double coverage, int maxUncoveredRun, int checkedLength) {
        return new AgentGuardrailVerdict(violation, coverage, maxUncoveredRun, checkedLength);
    }

    /**
     * 无指标可用时的违规（如 fail-closed 或规则命中）。
     */
    public static AgentGuardrailVerdict violation(AgentGuardrailViolation violation) {
        return new AgentGuardrailVerdict(violation, 0.0d, 0, 0);
    }

    public boolean isPassed() {
        return violation == null;
    }

    /**
     * 结构化短标识；通过时为 null。可安全写入审计与日志。
     */
    public String reason() {
        return violation == null ? null : violation.reason();
    }

    /**
     * 供结构化日志使用的指标串，只含数值，不含任何文本内容。
     */
    public String metrics() {
        return String.format("coverage=%.3f maxUncoveredRun=%d checkedLength=%d",
                coverage, maxUncoveredRun, checkedLength);
    }
}
