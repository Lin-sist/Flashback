package com.flashback.domain;

/**
 * Agent 会话状态。
 */
public enum AgentSessionStatus {

    /** 进行中，可继续追加消息。 */
    ACTIVE,
    /** 已结束，只读，不可追加消息。 */
    ENDED
}
