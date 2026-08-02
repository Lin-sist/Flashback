package com.flashback.agent.reflection;

import com.flashback.agent.AgentRawToolCall;

import java.util.List;

/** Reply pipeline 的内部边界结果；不进入持久化。 */
public record AgentReply(
        boolean success,
        String content,
        String message,
        List<AgentRawToolCall> toolCalls) {

    public AgentReply {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AgentReply ok(String content, List<AgentRawToolCall> toolCalls) {
        return new AgentReply(true, content, null, toolCalls);
    }

    public static AgentReply fail(String message) {
        return new AgentReply(false, null, message, List.of());
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
