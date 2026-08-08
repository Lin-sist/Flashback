package com.flashback.agent.resilience;

/**
 * C8：provider 失败的封闭分类。
 *
 * <p>wire id 只用于脱敏 trace / 日志，不进入前端协议。{@code transientFailure}
 * 只表达排查属性，不授权自动重试，也不控制用户是否能主动重试 pending turn。</p>
 */
public enum AgentProviderFailureCategory {

    TIMEOUT("timeout", true),
    THROTTLED("throttled", true),
    AUTH_CONFIGURATION("auth-configuration", false),
    UPSTREAM_UNAVAILABLE("upstream-unavailable", true),
    INVALID_RESPONSE("invalid-response", false),
    REQUEST_REJECTED("request-rejected", false),
    INTERRUPTED("interrupted", false),
    UNKNOWN("unknown", false);

    private final String wireId;
    private final boolean transientFailure;

    AgentProviderFailureCategory(String wireId, boolean transientFailure) {
        this.wireId = wireId;
        this.transientFailure = transientFailure;
    }

    public String wireId() {
        return wireId;
    }

    public boolean isTransient() {
        return transientFailure;
    }
}
