package com.flashback.agent.guardrail;

/**
 * 护栏违规类型（C4）。
 *
 * 每个枚举值同时携带一个短标识，用于落入 agent_tool_call.failure_type
 * 与结构化日志（design.md §6：痕迹只含结构化标识，不含文本内容）。
 */
public enum AgentGuardrailViolation {

    /** 候选文本增写了用户没有表达过的内容（R1 的形态）。 */
    UNFAITHFUL("unfaithful"),

    /** 提议话术中引号包裹的伪引用在用户原话中无来源。 */
    FABRICATED_QUOTE("fabricated-quote"),

    /** Agent 在自己新增的表述中给出诊断性判断或医学建议。 */
    DIAGNOSTIC("diagnostic"),

    /** Agent 谎称已代替用户完成封存 / 解锁 / 删除等不可逆操作。 */
    FAKE_ACTION("fake-action"),

    /** 判定过程自身异常，按 fail-closed 处理。 */
    CHECK_ERROR("check-error");

    private final String reason;

    AgentGuardrailViolation(String reason) {
        this.reason = reason;
    }

    /**
     * 结构化短标识，可安全写入审计与日志。
     */
    public String reason() {
        return reason;
    }
}
