package com.flashback.domain;

/**
 * Agent 写作引导阶段。
 *
 * C1 使用后端显式阶段机，不把节奏控制权交给模型
 * （见 openspec/changes/agent-runtime-mvp/design.md 决策 4）。
 */
public enum AgentStage {

    /** 开场，尚未开始引导。 */
    OPENING,
    /** 引导用户描述此刻情绪。 */
    EMOTION,
    /** 引导用户说出困惑所在。 */
    CONFUSION,
    /** 引导用户收敛出核心问题。 */
    CORE_QUESTION,
    /** 引导用户表达期望。 */
    EXPECTATION,
    /** 收束，产出素材草稿。 */
    CLOSING,
    /**
     * C3b：友人回看对话的固定阶段。
     *
     * 回看**无阶段机**（Q4 定稿）：自由多轮 + 轮次上限，阶段恒为本常量。
     *
     * 为什么用专用常量而不复用 OPENING 或 CLOSING（design 决策 2）：
     * - 复用 OPENING 会让人误以为回看也有阶段推进、只是卡在第一阶段；
     * - 复用 CLOSING 更危险——它在写作引导里是**会触发素材生成**的阶段
     * （AgentChatServiceImpl 里 targetStage == CLOSING 即调 generateMaterial），
     * 复用等于给自己埋一个「回看意外产出可回填正文素材」的坑。
     *
     * 本常量**不参与** AgentStageMachine 的任何推进逻辑。
     */
    REVIEW,
    /** 会话已结束，只读。 */
    ENDED
}
