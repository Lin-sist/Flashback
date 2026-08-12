package com.flashback.domain;

import java.time.LocalDateTime;

public class DataOperation {
    private Long id;
    private Long userId;
    private DataOperationType operationType;
    private DataOperationStatus status;
    private SealedContentPolicy sealedContentPolicy;
    private int totalItems;
    private int processedItems;
    private int failedItems;
    private String confirmationNonceHash;
    private LocalDateTime confirmationExpiresAt;
    private String artifactToken;
    private LocalDateTime artifactExpiresAt;
    private DataOperationFailureCode failureCode;
    private LocalDateTime confirmedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public DataOperationType getOperationType() { return operationType; }
    public void setOperationType(DataOperationType operationType) { this.operationType = operationType; }
    public DataOperationStatus getStatus() { return status; }
    public void setStatus(DataOperationStatus status) { this.status = status; }
    public SealedContentPolicy getSealedContentPolicy() { return sealedContentPolicy; }
    public void setSealedContentPolicy(SealedContentPolicy sealedContentPolicy) { this.sealedContentPolicy = sealedContentPolicy; }
    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }
    public int getProcessedItems() { return processedItems; }
    public void setProcessedItems(int processedItems) { this.processedItems = processedItems; }
    public int getFailedItems() { return failedItems; }
    public void setFailedItems(int failedItems) { this.failedItems = failedItems; }
    public String getConfirmationNonceHash() { return confirmationNonceHash; }
    public void setConfirmationNonceHash(String confirmationNonceHash) { this.confirmationNonceHash = confirmationNonceHash; }
    public LocalDateTime getConfirmationExpiresAt() { return confirmationExpiresAt; }
    public void setConfirmationExpiresAt(LocalDateTime confirmationExpiresAt) { this.confirmationExpiresAt = confirmationExpiresAt; }
    public String getArtifactToken() { return artifactToken; }
    public void setArtifactToken(String artifactToken) { this.artifactToken = artifactToken; }
    public LocalDateTime getArtifactExpiresAt() { return artifactExpiresAt; }
    public void setArtifactExpiresAt(LocalDateTime artifactExpiresAt) { this.artifactExpiresAt = artifactExpiresAt; }
    public DataOperationFailureCode getFailureCode() { return failureCode; }
    public void setFailureCode(DataOperationFailureCode failureCode) { this.failureCode = failureCode; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
