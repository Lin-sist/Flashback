package com.flashback.agent.memory;

import com.flashback.domain.AgentSessionPurpose;

import java.util.List;

/**
 * 记忆检索请求（C3 agent-memory-retrieval）。
 *
 * 为什么带 {@code purpose}（design.md 决策 8）：写作引导与友人回看对这段历史的需求不同——
 * 写作引导要找「和此刻类似的过去」，回看要找「和这条记录相关的前后文」。
 * 后一刀 agent-review-chat 必须复用同一个 Port，
 * 若签名里没有用途维度，那一刀就得改 Port 签名，
 * 而 Port 的稳定性正是拆两刀时最该保护的东西。
 *
 * 为什么带 {@code excludeRecordId}：会话正在写的那条记录不该被当成「过去的记忆」，
 * 否则 Agent 会把用户此刻正在写的内容当作旧事复述。
 *
 * 隐私：{@code keywords} 来自用户此刻的表达，只在内存中存在，不落库、不写日志。
 *
 * @param userId          必填，检索恒按用户隔离
 * @param purpose         会话用途
 * @param keywords        检索关键词（已过滤长度与数量）
 * @param tagIds          当前草稿已绑定的标签，用于同标签关联
 * @param excludeRecordId 需要排除的记录（通常是当前会话绑定的草稿），可为 null
 * @param limit           最多返回条数
 */
public record MemoryQuery(
        Long userId,
        AgentSessionPurpose purpose,
        List<String> keywords,
        List<Long> tagIds,
        Long excludeRecordId,
        int limit) {

    public MemoryQuery {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    /**
     * 是否存在任何可用的检索线索。
     *
     * 无线索时不应发起查询——否则会退化成「按时间倒序取最近几条」，
     * 那不是记忆关联，而是随机翻旧账，正好违反「不得为显得有记忆而编造关联」。
     */
    public boolean hasCue() {
        return !keywords.isEmpty() || !tagIds.isEmpty();
    }
}
