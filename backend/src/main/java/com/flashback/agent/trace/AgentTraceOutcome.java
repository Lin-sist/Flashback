package com.flashback.agent.trace;

/**
 * 一轮对话的最终结果（C5）。
 *
 * 与 {@code AgentSessionVO.status} 的关系：前者是给用户看的状态，本枚举是给开发者看的轨迹结论。
 * 刻意多出 {@code DOWNGRADED} 一项——对用户来说护栏降级是一次「成功返回」
 * （他确实收到了一句回复），但排查时必须能一眼看出这句话不是 provider 的正常产出。
 * 把两者混成 SUCCESS 会让轨迹丢掉 C5 最想观测的那类事件。
 */
public enum AgentTraceOutcome {

    /** 正常完成，回复来自 provider 或 mock 的正常产出。 */
    SUCCESS,

    /** 护栏降级：用户收到了回复，但那是本地兜底常量。 */
    DOWNGRADED,

    /** provider 调用失败或返回无效内容；用户消息保留，可同轮重试。 */
    FAILED,

    /** provider 未配置或不可用，本轮未发起调用。 */
    UNAVAILABLE
}
