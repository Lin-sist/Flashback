package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * Agent 会话消息实体。
 *
 * 隐私约定：content 为高敏业务数据（等同用户日记原文），
 * 只允许存在于 agent_message 表，禁止写入应用日志 / telemetry / tracked files。
 */
public class AgentMessage {

    private Long id;
    private Long sessionId;
    private Long userId;
    private AgentMessageRole role;
    private int turnNo;
    private AgentStage stage;
    private String content;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AgentMessageRole getRole() {
        return role;
    }

    public void setRole(AgentMessageRole role) {
        this.role = role;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public AgentStage getStage() {
        return stage;
    }

    public void setStage(AgentStage stage) {
        this.stage = stage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
