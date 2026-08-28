package com.flashback.agent.safety;

/**
 * R1 安全判定只暴露封闭枚举，不携带命中片段、概率或诊断。
 */
public record AgentSafetyDecision(AgentSafetyLevel level, AgentSafetyRule rule) {

    public static AgentSafetyDecision none() {
        return new AgentSafetyDecision(AgentSafetyLevel.NONE, AgentSafetyRule.NONE);
    }

    public boolean intervenes() {
        return level == AgentSafetyLevel.IMMEDIATE_SELF_HARM;
    }
}
