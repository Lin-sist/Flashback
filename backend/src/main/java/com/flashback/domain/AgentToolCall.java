package com.flashback.domain;

import com.flashback.agent.tool.AgentToolCallStatus;

import java.time.LocalDateTime;

/**
 * Agent 工具提议 / 执行实体（C2）。
 *
 * 归属约定：userId 为唯一归属依据，跨用户访问必须被拒绝。
 *
 * 隐私约定（design.md 决策 6）：argsDigest 只存参数的结构化摘要，
 * **不得**存放用户日记原文或对话原文。askText 是 Agent 自己的征询话术，
 * 属于 Agent 输出而非用户日记，因此可落库。
 */
public class AgentToolCall {

    private Long id;
    private Long sessionId;
    private Long userId;
    private Long recordId;
    private int turnNo;
    private String toolName;
    private AgentToolCallStatus status;
    private String argsDigest;
    /**
     * 瞬态执行参数缓冲，仅在 PROPOSED 期间有值，确认后即被清空。
     * 见 c2-agent-tool-call.sql 中 pending_args 的说明。
     */
    private String pendingArgs;
    private String askText;
    private String failureType;
    private String resultSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public AgentToolCallStatus getStatus() {
        return status;
    }

    public void setStatus(AgentToolCallStatus status) {
        this.status = status;
    }

    public String getArgsDigest() {
        return argsDigest;
    }

    public void setArgsDigest(String argsDigest) {
        this.argsDigest = argsDigest;
    }

    public String getPendingArgs() {
        return pendingArgs;
    }

    public void setPendingArgs(String pendingArgs) {
        this.pendingArgs = pendingArgs;
    }

    public String getAskText() {
        return askText;
    }

    public void setAskText(String askText) {
        this.askText = askText;
    }

    public String getFailureType() {
        return failureType;
    }

    public void setFailureType(String failureType) {
        this.failureType = failureType;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
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
