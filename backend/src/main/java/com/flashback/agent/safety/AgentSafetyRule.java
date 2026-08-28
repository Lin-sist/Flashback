package com.flashback.agent.safety;

/** 结构化规则标识；不得承载用户文本。 */
public enum AgentSafetyRule {
    NONE("none"),
    DIRECT_INTENT("direct-intent"),
    IMMINENT_PLAN("imminent-plan"),
    ATTEMPT_IN_PROGRESS("attempt-in-progress");

    private final String id;

    AgentSafetyRule(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
