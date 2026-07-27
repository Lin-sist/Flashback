package com.flashback.agent;

import java.util.List;

/**
 * provider function calling 响应的解析结果（C2）。
 *
 * 两个字段天然并存（proposal F24）：content 是 Agent 的自然语言回复，
 * toolCalls 是原生工具提议。FC 场景下 content 可能为空，
 * 此时由上层用提议的 askText 作为该轮回复兜底（design 数据流 2.1 要点二）。
 *
 * @param content   自然语言回复；可能为 null
 * @param toolCalls 原始工具提议；可能为空
 */
public record AgentModelResponse(String content, List<AgentRawToolCall> toolCalls) {

    public AgentModelResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * 首个工具提议；无提议时返回 null。
     * 单轮至多处理一个提议（design 决策 10）。
     */
    public AgentRawToolCall firstToolCall() {
        return toolCalls.isEmpty() ? null : toolCalls.get(0);
    }
}
