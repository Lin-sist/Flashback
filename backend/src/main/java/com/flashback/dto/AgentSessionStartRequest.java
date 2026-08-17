package com.flashback.dto;

import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentConversationIntent;

/**
 * 开启或恢复 Agent 会话请求。
 *
 * recordId：写作引导时可选（为空表示不与具体记录关联）；回看时必填。
 *
 * C3b：新增 purpose。复用本端点而非另开一套回看端点（design 决策 6）——
 * 会话的读取、追加消息、结束三个端点在两种模式下语义完全一致，
 * 若开会话另起一套，紧接着就要回答那三个要不要也复制。
 * 缺省 WRITING_GUIDANCE，既有前端调用无需改动。
 */
public class AgentSessionStartRequest {

    private Long recordId;

    private AgentSessionPurpose purpose;

    /** P4.1：只对 WRITING_GUIDANCE 有效；缺省为最少打扰的 LISTEN。 */
    private AgentConversationIntent conversationIntent;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public AgentSessionPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(AgentSessionPurpose purpose) {
        this.purpose = purpose;
    }

    public AgentConversationIntent getConversationIntent() {
        return conversationIntent;
    }

    public void setConversationIntent(AgentConversationIntent conversationIntent) {
        this.conversationIntent = conversationIntent;
    }

    /**
     * 归一化后的用途：未指定视为写作引导，保证既有调用行为不变。
     */
    public AgentSessionPurpose purposeOrDefault() {
        return purpose == null ? AgentSessionPurpose.WRITING_GUIDANCE : purpose;
    }

    public AgentConversationIntent conversationIntentOrDefault() {
        return conversationIntent == null ? AgentConversationIntent.LISTEN : conversationIntent;
    }
}
