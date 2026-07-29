package com.flashback.agent;

import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.RecordStatus;

/**
 * 对话模式（C3b agent-review-chat）。
 *
 * 存在理由（design.md 决策 1 的硬约束）：写作引导与友人回看共享绝大部分链路
 * ——会话与消息持久化、归属校验、失败重试语义、provider 调用、来源分层、
 * 以及忠实度 / 诊断 / 代决 / 伪引用 / 时间归属五层护栏。
 * 差异只有四处：作用对象的记录状态、是否走阶段机、是否下发工具、是否产出素材。
 *
 * 之所以不把这四处写成四个散落的 {@code if (purpose == REVIEW_CHAT)}：
 * 那样一来「这条链路在两种模式下分别做什么」就要靠通读整个编排方法才能回答，
 * 而下一次改护栏时极容易只改到一边。本枚举把四个问题集中在一处回答，
 * 编排方法只问模式、不问 purpose。
 *
 * 之所以不为回看新建一个 Service：那会让上面五层护栏在两条链路各接一遍，
 * 重演 C4 决策 5 已经修掉的「护栏规则分散在多处」问题。
 */
public enum AgentChatMode {

    /** 「写下此刻」的多轮写作引导（C1 起的既有行为）。 */
    WRITING_GUIDANCE(RecordStatus.DRAFT, true, true, true),

    /**
     * 解锁后的友人回看对话。
     *
     * 三个 false 都是产品约束而非实现简化：
     * - 无阶段机：回看是读后闲聊，硬套六阶段引导会变成盘问；
     * - 无工具：已解锁记录没有任何合法写操作（封存后 location/attachments/cover
     * 不可变，正文也不该被回看追加）；
     * - 无素材：往已解锁记录的正文里追加此刻的整理会破坏它的时间完整性——
     * 用户几个月后无法分辨哪句是当时写的、哪句是回看时补的。
     */
    REVIEW_CHAT(RecordStatus.UNLOCKED, false, false, false);

    private final RecordStatus requiredRecordStatus;
    private final boolean stageMachineDriven;
    private final boolean toolsAvailable;
    private final boolean materialProduced;

    AgentChatMode(
            RecordStatus requiredRecordStatus,
            boolean stageMachineDriven,
            boolean toolsAvailable,
            boolean materialProduced) {
        this.requiredRecordStatus = requiredRecordStatus;
        this.stageMachineDriven = stageMachineDriven;
        this.toolsAvailable = toolsAvailable;
        this.materialProduced = materialProduced;
    }

    /**
     * 由会话用途派生模式。
     *
     * null 兜底为写作引导，与 {@code AgentSession.setPurpose} 的兜底一致——
     * 读到历史数据或列缺失时不应得到一个无模式的会话。
     */
    public static AgentChatMode of(AgentSessionPurpose purpose) {
        return purpose == AgentSessionPurpose.REVIEW_CHAT ? REVIEW_CHAT : WRITING_GUIDANCE;
    }

    /**
     * 该模式要求目标记录处于哪个状态。
     *
     * 写作引导要求 DRAFT（封存后不可变契约的延续）；
     * 回看要求 UNLOCKED——SEALED 未解锁的记录用户自己都还没到能看的时刻，
     * Agent 陪他聊它等于替时间拆封。
     */
    public RecordStatus requiredRecordStatus() {
        return requiredRecordStatus;
    }

    /** 该模式是否由 {@link AgentStageMachine} 推进阶段。 */
    public boolean isStageMachineDriven() {
        return stageMachineDriven;
    }

    /**
     * 该模式下是否允许向 provider 下发工具定义。
     *
     * 这个方法存在的具体理由：{@code buildToolContext} 原先只按「有无 recordId」
     * 判断是否下发 tools，而回看会话**恰好绑定一条记录**——
     * 也就是说「回看无工具」不会自动成立，必须显式短路。
     */
    public boolean areToolsAvailable() {
        return toolsAvailable;
    }

    /** 该模式收束时是否产出可回填正文的素材草稿。 */
    public boolean isMaterialProduced() {
        return materialProduced;
    }

    /** 该模式下会话是否需要绑定记录。回看没有记录可回看，故必填。 */
    public boolean requiresRecord() {
        return this == REVIEW_CHAT;
    }
}
