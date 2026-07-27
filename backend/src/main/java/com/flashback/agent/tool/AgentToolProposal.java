package com.flashback.agent.tool;

import java.util.List;

/**
 * 由 provider 响应的 tool_calls 解析出的结构化提议（C2）。
 *
 * 关键语义（design.md 关键不变量 1、决策 2）：
 * - 提议只是**待确认意图**，本身不产生任何写操作；
 * - 执行只能由用户确认的独立请求驱动，后端不在生成回复的同一处理过程内执行。
 *
 * @param tool     命中的白名单工具
 * @param askText  征询话术，也用作 provider 未返回 content 时的该轮回复兜底
 * @param text     append_record_content 的素材（其他工具为 null）
 * @param tagIds   add_record_tags 的标签 id（其他工具为空）
 * @param unlockAt propose_unlock_at 的解锁时间原文（其他工具为 null）
 */
public record AgentToolProposal(
        AgentToolName tool,
        String askText,
        String text,
        List<Long> tagIds,
        String unlockAt) {

    public AgentToolProposal {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    public static AgentToolProposal appendContent(String askText, String text) {
        return new AgentToolProposal(AgentToolName.APPEND_RECORD_CONTENT, askText, text, List.of(), null);
    }

    public static AgentToolProposal addTags(String askText, List<Long> tagIds) {
        return new AgentToolProposal(AgentToolName.ADD_RECORD_TAGS, askText, null, tagIds, null);
    }

    public static AgentToolProposal proposeUnlockAt(String askText, String unlockAt) {
        return new AgentToolProposal(AgentToolName.PROPOSE_UNLOCK_AT, askText, null, List.of(), unlockAt);
    }
}
