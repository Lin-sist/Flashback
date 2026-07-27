package com.flashback.agent.tool;

/**
 * 提议校验结果（C2）。
 *
 * 语义（design.md 数据流 2.1 要点一）：
 * 校验失败**不让整轮失败**——Agent 该说的话照说，只是不下发确认条。
 * 模型幻觉一个工具名不该毁掉用户这一轮的对话体验。
 *
 * @param proposal     校验通过的提议（失败时为 null）
 * @param rejectReason 拒绝原因短标识（通过时为 null）
 */
public record AgentToolValidationResult(
        AgentToolProposal proposal,
        String rejectReason) {

    /** 工具名不在白名单内，或命中的是不可提议的读工具。 */
    public static final String REASON_NOT_ALLOWLISTED = "not-allowlisted";

    /** 参数缺失或类型不符。 */
    public static final String REASON_INVALID_ARGUMENT = "invalid-argument";

    /** 参数超出代码层边界（长度、数量、时序）。 */
    public static final String REASON_OUT_OF_BOUNDS = "out-of-bounds";

    /** 会话未绑定可编辑草稿，写工具无作用对象。 */
    public static final String REASON_NO_DRAFT_CONTEXT = "no-draft-context";

    /** 同轮已有一个合法提议，本条被丢弃（design 决策 10）。 */
    public static final String REASON_SUPERSEDED = "superseded";

    public static AgentToolValidationResult accepted(AgentToolProposal proposal) {
        return new AgentToolValidationResult(proposal, null);
    }

    public static AgentToolValidationResult rejected(String reason) {
        return new AgentToolValidationResult(null, reason);
    }

    public boolean isAccepted() {
        return proposal != null;
    }
}
