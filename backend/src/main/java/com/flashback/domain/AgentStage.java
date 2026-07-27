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
    /** 会话已结束，只读。 */
    ENDED
}
