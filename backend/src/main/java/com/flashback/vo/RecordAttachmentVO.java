package com.flashback.vo;

import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;

import java.time.LocalDateTime;

/**
 * M4 record attachment response.
 */
public class RecordAttachmentVO {

    private Long id;
    private Long recordId;
    private RecordAttachmentType type;
    private RecordAttachmentStatus status;
    private String fileName;
    private String mimeType;
    private Long sizeBytes;
    private Integer width;
    private Integer height;
    private Integer durationSeconds;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private String accessUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public RecordAttachmentType getType() {
        return type;
    }

    public void setType(RecordAttachmentType type) {
        this.type = type;
    }

    public RecordAttachmentStatus getStatus() {
        return status;
    }

    public void setStatus(RecordAttachmentStatus status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }
}
