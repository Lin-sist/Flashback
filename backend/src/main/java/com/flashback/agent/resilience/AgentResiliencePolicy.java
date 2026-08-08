package com.flashback.agent.resilience;

import com.flashback.domain.AgentStage;

/**
 * C8：固定、克制的失败呈现映射。
 *
 * <p>方法刻意不接收用户文本、prompt、异常或 provider response，避免模板被自由文本污染。</p>
 */
public final class AgentResiliencePolicy {

    private static final String OPENING_RETRYABLE = "我现在还没能接上，可以稍后再试一次。";
    private static final String OPENING_UNAVAILABLE = "我现在暂时无法接上，请稍后再回来。";
    private static final String TURN_RETRYABLE = "刚才写下的这句还在。我现在没能接上，可以再试一次。";
    private static final String TURN_UNAVAILABLE = "刚才写下的这句还在，但现在暂时无法继续。";

    public String failureMessage(
            String operation,
            AgentStage stage,
            AgentProviderFailureCategory category) {
        if (operation != null && operation.startsWith("opening")) {
            return safe(category).isTransient() ? OPENING_RETRYABLE : OPENING_UNAVAILABLE;
        }
        return safe(category).isTransient() ? TURN_RETRYABLE : TURN_UNAVAILABLE;
    }

    private AgentProviderFailureCategory safe(AgentProviderFailureCategory category) {
        return category == null ? AgentProviderFailureCategory.UNKNOWN : category;
    }
}
