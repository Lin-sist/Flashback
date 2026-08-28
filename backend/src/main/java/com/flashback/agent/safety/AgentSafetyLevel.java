package com.flashback.agent.safety;

/** R1：当前 turn 的窄安全判定；不持久化为用户标签。 */
public enum AgentSafetyLevel {
    NONE("none"),
    IMMEDIATE_SELF_HARM("immediate-self-harm");

    private final String id;

    AgentSafetyLevel(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
