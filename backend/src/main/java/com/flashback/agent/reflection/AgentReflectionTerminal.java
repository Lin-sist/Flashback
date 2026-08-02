package com.flashback.agent.reflection;

/** Reflection 的脱敏终态；不得携带候选文本或 provider 响应。 */
public enum AgentReflectionTerminal {

    REWRITTEN("rewritten"),
    FALLBACK("fallback"),
    INVALID_CONTENT("invalid-content"),
    PROVIDER_FAILED("provider-failed");

    private final String id;

    AgentReflectionTerminal(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
