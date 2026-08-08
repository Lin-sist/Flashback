package com.flashback.agent.resilience;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

/** C8 provider failure 分类器：只看异常类型与 HTTP status，不解析自由文本。 */
public final class AgentProviderFailures {

    private AgentProviderFailures() {
    }

    public static AgentProviderFailureCategory fromHttpStatus(int status) {
        if (status == 401 || status == 403) {
            return AgentProviderFailureCategory.AUTH_CONFIGURATION;
        }
        if (status == 429) {
            return AgentProviderFailureCategory.THROTTLED;
        }
        if (status >= 500 && status <= 599) {
            return AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE;
        }
        if (status >= 400 && status <= 499) {
            return AgentProviderFailureCategory.REQUEST_REJECTED;
        }
        return AgentProviderFailureCategory.UNKNOWN;
    }

    public static AgentProviderFailureCategory fromThrowable(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof AgentProviderException providerException) {
                return providerException.category();
            }
            if (current instanceof HttpTimeoutException) {
                return AgentProviderFailureCategory.TIMEOUT;
            }
            if (current instanceof ConnectException) {
                return AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE;
            }
            if (current instanceof InterruptedException) {
                return AgentProviderFailureCategory.INTERRUPTED;
            }
            current = current.getCause();
        }
        return AgentProviderFailureCategory.UNKNOWN;
    }

    public static AgentProviderException typed(Throwable failure) {
        if (failure instanceof AgentProviderException providerException) {
            return providerException;
        }
        return AgentProviderException.of(fromThrowable(failure), failure);
    }
}
