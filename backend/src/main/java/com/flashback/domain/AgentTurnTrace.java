package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * Agent 一轮对话的决策轨迹（C5）。
 *
 * 隐私（design.md §2.3）：本实体所有字段只承载结构化标识、数值指标与不可还原摘要。
 * **禁止**新增任何承载用户日记原文、对话原文、记忆片段内容、护栏候选文本、
 * 提示词全文或 provider 响应体的字段——本表不是这些数据的授权存储。
 */
public class AgentTurnTrace {

    private Long id;
    /** 一轮一个，供 C6 关联同一轮的多处数据。 */
    private String traceId;
    private Long sessionId;
    private Long userId;
    private Long recordId;
    private Integer turnNo;
    /**
     * 同轮重试的第几次尝试。
     *
     * (sessionId, turnNo) 在 provider 失败重试时会重复，故单列此字段区分
     * 「重试」与「新一轮」——否则轨迹里两次尝试会长得一模一样。
     */
    private Integer attemptNo;
    private String purpose;
    private String stage;
    /** 阶段判定结论；回看无阶段机时为 null，不伪造。 */
    private String stageReason;
    private String model;
    private String promptVersion;
    private String policyVersion;
    private String outcome;
    private Long providerDurationMs;
    /** 只存异常类名或结构化原因标识，不存异常消息。 */
    private String causeType;
    private String downgradePath;
    private String violation;
    /** 步骤明细的结构化 JSON；不是自由文本容器。 */
    private String stepsJson;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
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

    public Integer getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(Integer turnNo) {
        this.turnNo = turnNo;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStageReason() {
        return stageReason;
    }

    public void setStageReason(String stageReason) {
        this.stageReason = stageReason;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Long getProviderDurationMs() {
        return providerDurationMs;
    }

    public void setProviderDurationMs(Long providerDurationMs) {
        this.providerDurationMs = providerDurationMs;
    }

    public String getCauseType() {
        return causeType;
    }

    public void setCauseType(String causeType) {
        this.causeType = causeType;
    }

    public String getDowngradePath() {
        return downgradePath;
    }

    public void setDowngradePath(String downgradePath) {
        this.downgradePath = downgradePath;
    }

    public String getViolation() {
        return violation;
    }

    public void setViolation(String violation) {
        this.violation = violation;
    }

    public String getStepsJson() {
        return stepsJson;
    }

    public void setStepsJson(String stepsJson) {
        this.stepsJson = stepsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
