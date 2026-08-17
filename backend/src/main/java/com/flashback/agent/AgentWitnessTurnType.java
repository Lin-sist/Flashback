package com.flashback.agent;

/**
 * P4.1：一轮见证型回复允许采用的封闭策略。
 *
 * 枚举只描述后端计算后的动作边界，不承载用户文本或模型候选内容。
 */
public enum AgentWitnessTurnType {
    /** 只回应已经听见的内容，不提问题。 */
    REFLECT_ONLY,
    /** 先回应；确有必要时至多提一个可跳过的问题。 */
    MAY_ASK_ONE,
    /** 温和收束，不提问题。 */
    CLOSE
}
