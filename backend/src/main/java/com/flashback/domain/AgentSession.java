package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * Agent 会话实体。
 *
 * 归属约定：userId 为唯一归属依据，跨用户访问必须被拒绝。
 */
public class AgentSession {

    private Long id;
    private Long userId;
    private Long recordId;
    /**
     * C3：会话用途。本刀只产生 WRITING_GUIDANCE；
     * 变更前创建的历史会话按 DDL 默认值同样视为 WRITING_GUIDANCE。
     */
    private AgentSessionPurpose purpose = AgentSessionPurpose.WRITING_GUIDANCE;
    /**
     * P4.1：仅 WRITING_GUIDANCE 使用；REVIEW_CHAT 保持 null。
     * 写作会话的安全缺省由 service 归一，避免 MyBatis 读取 SQL NULL 时保留字段初始化值。
     */
    private AgentConversationIntent conversationIntent;
    private AgentStage stage;
    private AgentSessionStatus status;
    private int turnCount;
    private int stageReaskCount;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
        // null 兜底为写作引导：读取历史数据或列缺失时不应得到一个无用途的会话。
        this.purpose = purpose == null ? AgentSessionPurpose.WRITING_GUIDANCE : purpose;
    }

    public AgentConversationIntent getConversationIntent() {
        return conversationIntent;
    }

    public void setConversationIntent(AgentConversationIntent conversationIntent) {
        this.conversationIntent = conversationIntent;
    }

    public AgentStage getStage() {
        return stage;
    }

    public void setStage(AgentStage stage) {
        this.stage = stage;
    }

    public AgentSessionStatus getStatus() {
        return status;
    }

    public void setStatus(AgentSessionStatus status) {
        this.status = status;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public int getStageReaskCount() {
        return stageReaskCount;
    }

    public void setStageReaskCount(int stageReaskCount) {
        this.stageReaskCount = stageReaskCount;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
