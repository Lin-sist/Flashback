package com.flashback.agent.trace;

/**
 * 护栏判定在哪一层发生（C5）。
 *
 * 存在理由：`AgentGuardrailVerdict` 只说「判定结论是什么」，不说「这是哪一道闸」。
 * 排查时这两件事必须都在，否则看到一个 `unfaithful` 也不知道被拦的是回复、素材还是工具参数——
 * 而这三者的处置方式完全不同（兜底替换 / 丢弃 / 拒绝提议）。
 *
 * 短标识用于落库与 steps_json，不含任何文本内容。
 */
public enum AgentTraceLayer {

    /** 回复路径的诊断 / 代决检查。 */
    REPLY_CONTENT("reply-content"),

    /** 回复路径的时间归属检查（C3）。 */
    REPLY_ATTRIBUTION("reply-attribution"),

    /** 回复路径的时间解释越界检查（C9）。 */
    REPLY_TEMPORAL("reply-temporal"),

    /** P4.1：回复问题数 0/1 上限检查。 */
    REPLY_QUESTION_LIMIT("reply-question-limit"),

    /** 素材路径的忠实度检查。 */
    MATERIAL_FAITHFULNESS("material-faithfulness"),

    /** 素材路径的诊断 / 代决检查。 */
    MATERIAL_CONTENT("material-content"),

    /** 回复长度硬上限。 */
    REPLY_LENGTH("reply-length"),

    /** 工具参数与提议话术的校验（含忠实度、伪引用、时间归属）。 */
    TOOL_ARGUMENTS("tool-arguments");

    private final String id;

    AgentTraceLayer(String id) {
        this.id = id;
    }

    /**
     * 结构化短标识，可安全写入轨迹与日志。
     */
    public String id() {
        return id;
    }
}
