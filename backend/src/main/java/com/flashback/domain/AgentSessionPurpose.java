package com.flashback.domain;

/**
 * Agent 会话用途（C3 agent-memory-retrieval 引入）。
 *
 * 为什么在本刀就引入（design.md 决策 8）：
 * C3 被拆成两刀，后一刀 agent-review-chat 要复用 agent_session 承载回看对话
 * （用户 Q4 定稿：复用而非另建表）。用途维度同时是 MemoryPort 的入参——
 * 写作引导要找「和此刻类似的过去」，回看要找「和这条记录相关的前后文」。
 * 若本刀不引入，Port 签名里就会有一个当时无处取值的参数，
 * 或者后一刀被迫改 Port 签名，而 Port 的稳定性正是拆两刀时最该保护的东西。
 *
 * 本刀只落地 {@link #WRITING_GUIDANCE}：
 * {@link #REVIEW_CHAT} 是**声明而非实现**，后端不得存在任何依赖它的行为分支。
 */
public enum AgentSessionPurpose {

    /** 「写下此刻」的多轮写作引导。C1 起的既有行为，本刀的默认值。 */
    WRITING_GUIDANCE,

    /**
     * 解锁后的友人回看对话。**本刀不实现**，留给 agent-review-chat。
     *
     * 保留枚举值而不实现行为是刻意的：数据库列的取值集合应当一次定稿，
     * 避免后一刀再做一次 DDL。
     */
    REVIEW_CHAT
}
