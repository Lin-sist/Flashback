package com.flashback.agent.memory;

import java.util.List;

/**
 * 记忆检索端口（C3 agent-memory-retrieval）。
 *
 * 存在理由（蓝图 D7、v1.2 草案「反推倒底线」第 4 条：
 * Memory / Trace / Eval 以 Port 演进，不绑死单一向量或 SaaS）：
 * 本刀的实现是 MySQL 上的标签 + 时间窗 + 结构化字段匹配，相关性弱是已接受的事实。
 * 将来若要换检索方式，换的应该只是实现类——
 * 调用方（Runtime 的上下文组装）不该知道底层是 LIKE 还是别的什么。
 *
 * 契约要求（agent-runtime delta）：
 * - 结果恒只含 {@code query.userId} 自己的记录，无任何跨用户分支；
 * - 不检索已封存但尚未解锁的记录——用户自己都还没到能看的时刻；
 * - 检索失败时由实现方决定是抛出还是返回空，
 * 但调用方必须按「不注入、对话继续」处理（design.md 决策 6 的 fail-open 方向）。
 */
public interface MemoryPort {

    /**
     * 检索与当前对话相关的历史记录片段。
     *
     * @param query 检索请求
     * @return 按相关性与时间排序的片段列表；无线索或无命中时返回空列表
     */
    List<MemoryFragment> retrieve(MemoryQuery query);
}
