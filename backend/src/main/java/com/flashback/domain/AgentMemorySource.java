package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * P4.2：某条 assistant message 实际注入 prompt 的来源关系。
 *
 * 只存结构化 ID，不复制 fragment、摘要、正文、关键词或分数。
 */
public class AgentMemorySource {

    private Long id;
    private Long userId;
    private Long sessionId;
    private Long assistantMessageId;
    private Long sourceRecordId;
    private AgentMemorySourceKind sourceKind;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getAssistantMessageId() {
        return assistantMessageId;
    }

    public void setAssistantMessageId(Long assistantMessageId) {
        this.assistantMessageId = assistantMessageId;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(Long sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public AgentMemorySourceKind getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(AgentMemorySourceKind sourceKind) {
        this.sourceKind = sourceKind;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
