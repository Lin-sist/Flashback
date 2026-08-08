package com.flashback.agent.resilience;

import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AgentProviderFailureClassifierTest {

    @Test
    void shouldClassifyHttpStatusesWithoutReadingResponseBody() {
        assertThat(AgentProviderFailures.fromHttpStatus(401)).isEqualTo(AgentProviderFailureCategory.AUTH_CONFIGURATION);
        assertThat(AgentProviderFailures.fromHttpStatus(403)).isEqualTo(AgentProviderFailureCategory.AUTH_CONFIGURATION);
        assertThat(AgentProviderFailures.fromHttpStatus(429)).isEqualTo(AgentProviderFailureCategory.THROTTLED);
        assertThat(AgentProviderFailures.fromHttpStatus(503)).isEqualTo(AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE);
        assertThat(AgentProviderFailures.fromHttpStatus(400)).isEqualTo(AgentProviderFailureCategory.REQUEST_REJECTED);
        assertThat(AgentProviderFailures.fromHttpStatus(302)).isEqualTo(AgentProviderFailureCategory.UNKNOWN);
    }

    @Test
    void shouldClassifyTypedFailuresAndPreserveClosedUnknownFallback() {
        assertThat(AgentProviderFailures.fromThrowable(new HttpTimeoutException("sensitive")))
                .isEqualTo(AgentProviderFailureCategory.TIMEOUT);
        assertThat(AgentProviderFailures.fromThrowable(new ConnectException("sensitive")))
                .isEqualTo(AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE);
        assertThat(AgentProviderFailures.fromThrowable(new InterruptedException("sensitive")))
                .isEqualTo(AgentProviderFailureCategory.INTERRUPTED);
        assertThat(AgentProviderFailures.fromThrowable(new IllegalStateException("sensitive")))
                .isEqualTo(AgentProviderFailureCategory.UNKNOWN);
    }

    @Test
    void categoryShouldExposeOnlyStableMetadata() {
        assertThat(AgentProviderFailureCategory.TIMEOUT.wireId()).isEqualTo("timeout");
        assertThat(AgentProviderFailureCategory.TIMEOUT.isTransient()).isTrue();
        assertThat(AgentProviderFailureCategory.AUTH_CONFIGURATION.isTransient()).isFalse();
        assertThat(AgentProviderFailureCategory.values()).hasSize(8);
    }

    @Test
    void typedExceptionMessageShouldNeverCopyStatusOrDownstreamMessage() {
        AgentProviderException statusFailure = AgentProviderException.forHttpStatus(429);
        AgentProviderException causeFailure = AgentProviderException.of(
                AgentProviderFailureCategory.UNKNOWN,
                new IllegalStateException("secret response body and endpoint"));

        assertThat(statusFailure.category()).isEqualTo(AgentProviderFailureCategory.THROTTLED);
        assertThat(statusFailure.httpStatus()).isEqualTo(429);
        assertThat(statusFailure.getMessage()).doesNotContain("429");
        assertThat(causeFailure.getMessage())
                .doesNotContain("secret", "response body", "endpoint");
    }
}
