package com.flashback.agent.reflection;

/** Provider 子调用在同一业务轮中的阶段。 */
public enum AgentProviderPhase {

    INITIAL("initial"),
    REFLECTION("reflection"),
    MATERIAL("material");

    private final String id;

    AgentProviderPhase(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
