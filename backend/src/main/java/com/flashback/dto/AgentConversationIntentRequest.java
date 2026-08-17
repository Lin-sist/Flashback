package com.flashback.dto;

import com.flashback.domain.AgentConversationIntent;
import jakarta.validation.constraints.NotNull;

/** P4.1：用户显式切换 witness 会话意图。 */
public class AgentConversationIntentRequest {

    @NotNull(message = "conversationIntent不能为空")
    private AgentConversationIntent conversationIntent;

    public AgentConversationIntent getConversationIntent() {
        return conversationIntent;
    }

    public void setConversationIntent(AgentConversationIntent conversationIntent) {
        this.conversationIntent = conversationIntent;
    }
}
