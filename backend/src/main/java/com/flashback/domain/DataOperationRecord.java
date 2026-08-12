package com.flashback.domain;

import java.time.LocalDateTime;

public class DataOperationRecord {
    private Long id;
    private Long operationId;
    private Long userId;
    private Long recordId;
    private DataOperationItemStatus itemStatus;
    private int attemptCount;
    private DataOperationFailureCode failureCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperationId() { return operationId; }
    public void setOperationId(Long operationId) { this.operationId = operationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public DataOperationItemStatus getItemStatus() { return itemStatus; }
    public void setItemStatus(DataOperationItemStatus itemStatus) { this.itemStatus = itemStatus; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public DataOperationFailureCode getFailureCode() { return failureCode; }
    public void setFailureCode(DataOperationFailureCode failureCode) { this.failureCode = failureCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
