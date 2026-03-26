package com.flashback.vo;

import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;

import java.time.LocalDateTime;

/**
 * 记录详情视图。
 */
public class RecordDetailVO {

    private Long id;
    private String title;
    private String content;
    private RecordType recordType;
    private String coreQuestion;
    private RecordStatus status;
    private LocalDateTime unlockAt;
    private LocalDateTime sealedAt;
    private LocalDateTime unlockedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public LocalDateTime getUnlockAt() {
        return unlockAt;
    }

    public void setUnlockAt(LocalDateTime unlockAt) {
        this.unlockAt = unlockAt;
    }

    public LocalDateTime getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(LocalDateTime sealedAt) {
        this.sealedAt = sealedAt;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
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
