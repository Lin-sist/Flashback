package com.flashback.vo;

import com.flashback.domain.DataOperationFailureCode;
import com.flashback.domain.DataOperationStatus;
import com.flashback.domain.DataOperationType;
import com.flashback.domain.SealedContentPolicy;
import java.time.LocalDateTime;

public class DataOperationVO {
    private Long id;
    private DataOperationType operationType;
    private DataOperationStatus status;
    private SealedContentPolicy sealedContentPolicy;
    private int totalItems;
    private int processedItems;
    private int failedItems;
    private DataOperationFailureCode failureCode;
    private LocalDateTime confirmationExpiresAt;
    private LocalDateTime artifactExpiresAt;
    private String confirmationText;
    private boolean retryable;
    private boolean downloadable;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public DataOperationType getOperationType() { return operationType; } public void setOperationType(DataOperationType v) { operationType = v; }
    public DataOperationStatus getStatus() { return status; } public void setStatus(DataOperationStatus v) { status = v; }
    public SealedContentPolicy getSealedContentPolicy() { return sealedContentPolicy; } public void setSealedContentPolicy(SealedContentPolicy v) { sealedContentPolicy = v; }
    public int getTotalItems() { return totalItems; } public void setTotalItems(int v) { totalItems = v; }
    public int getProcessedItems() { return processedItems; } public void setProcessedItems(int v) { processedItems = v; }
    public int getFailedItems() { return failedItems; } public void setFailedItems(int v) { failedItems = v; }
    public DataOperationFailureCode getFailureCode() { return failureCode; } public void setFailureCode(DataOperationFailureCode v) { failureCode = v; }
    public LocalDateTime getConfirmationExpiresAt() { return confirmationExpiresAt; } public void setConfirmationExpiresAt(LocalDateTime v) { confirmationExpiresAt = v; }
    public LocalDateTime getArtifactExpiresAt() { return artifactExpiresAt; } public void setArtifactExpiresAt(LocalDateTime v) { artifactExpiresAt = v; }
    public String getConfirmationText() { return confirmationText; } public void setConfirmationText(String v) { confirmationText = v; }
    public boolean isRetryable() { return retryable; } public void setRetryable(boolean v) { retryable = v; }
    public boolean isDownloadable() { return downloadable; } public void setDownloadable(boolean v) { downloadable = v; }
}
