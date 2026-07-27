package com.flashback.agent.tool;

/**
 * 工具执行结果（C2）。
 *
 * 失败语义（design.md §5、proposal 验收 14）：
 * 失败必须显式，不得静默、不得谎报成功。failureType 为结构化短标识，
 * message 为可读原因，两者都不含用户日记原文。
 *
 * @param status      结果状态
 * @param failureType 失败类型短标识（成功时为 null）
 * @param message     可读消息，用于前端提示与对话上下文回注摘要
 */
public record AgentToolOutcome(
        AgentToolCallStatus status,
        String failureType,
        String message) {

    /** 执行被业务校验拒绝（记录已封存、标签非法、时间非法等）。 */
    public static final String FAILURE_BUSINESS_REJECTED = "business-rejected";

    /** 执行过程发生未预期异常。 */
    public static final String FAILURE_UNEXPECTED = "unexpected";

    /** 提议已失效（会话未绑定记录等前置条件不再成立）。 */
    public static final String FAILURE_PRECONDITION = "precondition-failed";

    public static AgentToolOutcome executed(String message) {
        return new AgentToolOutcome(AgentToolCallStatus.EXECUTED, null, message);
    }

    public static AgentToolOutcome failed(String failureType, String message) {
        return new AgentToolOutcome(AgentToolCallStatus.FAILED, failureType, message);
    }

    public static AgentToolOutcome rejected(String message) {
        return new AgentToolOutcome(AgentToolCallStatus.REJECTED, null, message);
    }

    public boolean isExecuted() {
        return status == AgentToolCallStatus.EXECUTED;
    }
}
