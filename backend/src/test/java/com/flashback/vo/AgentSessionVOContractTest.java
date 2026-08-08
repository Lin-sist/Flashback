package com.flashback.vo;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** C8 零契约回归：失败分类留在 backend，不扩张用户可见 DTO。 */
class AgentSessionVOContractTest {

    @Test
    void c8MustNotAddFailureCategoryOrRetryMetadataFields() {
        Set<String> fields = java.util.Arrays.stream(AgentSessionVO.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "sessionId",
                "recordId",
                "stage",
                "sessionStatus",
                "turnCount",
                "maxTurns",
                "canContinue",
                "messages",
                "materialDraft",
                "source",
                "status",
                "message",
                "pendingToolCall",
                "lastToolCallResult");
        assertThat(fields).doesNotContain("failureCategory", "retryable", "httpStatus", "deadline");
    }
}
