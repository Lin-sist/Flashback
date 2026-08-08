package com.flashback.agent.resilience;

import java.io.IOException;

/**
 * C8：不携带 provider payload 的类型化失败。
 *
 * <p>message 只由稳定 category 派生，绝不复制下游异常 message 或 response body。</p>
 */
public final class AgentProviderException extends IOException {

    private final AgentProviderFailureCategory category;
    private final Integer httpStatus;
    private final boolean deadlineExhausted;

    private AgentProviderException(
            AgentProviderFailureCategory category,
            Integer httpStatus,
            boolean deadlineExhausted,
            Throwable cause) {
        super("agent provider failure: " + safe(category).wireId(), cause);
        this.category = safe(category);
        this.httpStatus = httpStatus;
        this.deadlineExhausted = deadlineExhausted;
    }

    public static AgentProviderException of(AgentProviderFailureCategory category, Throwable cause) {
        return new AgentProviderException(category, null, false, cause);
    }

    public static AgentProviderException forHttpStatus(int status) {
        return new AgentProviderException(AgentProviderFailures.fromHttpStatus(status), status, false, null);
    }

    public static AgentProviderException invalidResponse(Throwable cause) {
        return new AgentProviderException(AgentProviderFailureCategory.INVALID_RESPONSE, null, false, cause);
    }

    public static AgentProviderException deadlineExhausted() {
        return new AgentProviderException(AgentProviderFailureCategory.TIMEOUT, null, true, null);
    }

    public AgentProviderFailureCategory category() {
        return category;
    }

    /** 仅限 backend 内部分支；不得写进用户响应或普通日志。 */
    public Integer httpStatus() {
        return httpStatus;
    }

    public boolean isDeadlineExhausted() {
        return deadlineExhausted;
    }

    private static AgentProviderFailureCategory safe(AgentProviderFailureCategory category) {
        return category == null ? AgentProviderFailureCategory.UNKNOWN : category;
    }
}
