package com.flashback.domain;

/**
 * P4.1：写作引导由用户明确选择的会话意图。
 *
 * 该值只属于 WRITING_GUIDANCE；REVIEW_CHAT 不伪造写作意图。
 */
public enum AgentConversationIntent {

    /** 只听见和回应，不主动提问。 */
    LISTEN,

    /** 帮用户梳理，每轮至多一个可跳过的问题。 */
    UNTANGLE
}
