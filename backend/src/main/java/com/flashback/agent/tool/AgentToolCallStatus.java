package com.flashback.agent.tool;

/**
 * 工具提议的生命周期状态（C2）。
 *
 * 流转（design.md 数据流 2.2）：
 * PROPOSED → EXECUTED（用户确认且执行成功）
 * PROPOSED → FAILED（用户确认但执行被业务校验拒绝或异常）
 * PROPOSED → REJECTED（用户拒绝）
 * REJECTED_BY_GUARD 为终态：提议在校验阶段即被后端拒绝，从未下发给用户。
 *
 * 幂等依据：只有 PROPOSED 可被确认，其余状态一律返回当前状态、不重复执行。
 */
public enum AgentToolCallStatus {

    /** 已提议，等待用户确认。 */
    PROPOSED,

    /** 用户确认并执行成功。 */
    EXECUTED,

    /** 用户确认但执行失败，目标记录未变更。 */
    FAILED,

    /** 用户拒绝，目标记录未变更。 */
    REJECTED,

    /** 后端校验阶段即拒绝（白名单外 / 参数非法 / 越界），从未下发给用户。 */
    REJECTED_BY_GUARD;

    public boolean isTerminal() {
        return this != PROPOSED;
    }
}
