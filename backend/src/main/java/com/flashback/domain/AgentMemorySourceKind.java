package com.flashback.domain;

/**
 * P4.2：实际进入 prompt 的来源类型。
 *
 * REVIEW_TARGET 是用户主动打开的回看记录；CROSS_RECORD 才受会话授权控制。
 */
public enum AgentMemorySourceKind {
    REVIEW_TARGET,
    CROSS_RECORD
}
