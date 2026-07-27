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
