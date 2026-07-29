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

    /**
     * C4：参数内容不忠实于用户原话（增写了用户没说过的内容）。
     *
     * 与上面几个原因的区别：它们校验「能否执行」（工具权限、参数形状、业务边界），
     * 本原因校验「内容是否忠实」——这正是 C2 拦不住 R1 的那道缝隙。
     */
    public static final String REASON_UNFAITHFUL_ARGS = "unfaithful-args";

    /** C4：提议话术中引号包裹的伪引用在用户原话中无来源。 */
    public static final String REASON_FABRICATED_QUOTE = "fabricated-quote";

    /** C4：提议话术中出现诊断性或谎报代决的表述。 */
    public static final String REASON_ASK_TEXT_VIOLATION = "ask-text-violation";

    /**
     * C3：正文参数的内容来自注入的历史记忆，而不是用户在本次对话中的表达。
     *
     * 与 REASON_UNFAITHFUL_ARGS 分开的理由：两者都不该放行，但成因不同。
     * 前者是「模型编了一句话」，后者是「模型把三个月前写的句子搬到今天的记录里」。
     * 分开留痕才能在闸门 3 观察到后者是否真的会发生——
     * 混成一个原因就只能看到「又被拒了一次」。
     */
    public static final String REASON_MEMORY_AS_CONTENT = "memory-as-content";

    /** C3：提议话术复述了历史内容却没说清那是过去哪个时候的事。 */
    public static final String REASON_MISSING_TIME_ATTRIBUTION = "missing-time-attribution";

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
