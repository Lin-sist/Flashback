package com.flashback.vo;

import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录列表项视图。
 */
public class RecordListItemVO {

    private Long id;
    private String title;
    private String contentPreview;
    private RecordType recordType;
    private RecordStatus status;
    private String lifeNodeLabel;
    private LocalDateTime unlockAt;
    private RecordAttachmentVO cover;
    private LocalDateTime createdAt;
    private List<String> tagNames;

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

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public String getLifeNodeLabel() {
        return lifeNodeLabel;
    }

    public void setLifeNodeLabel(String lifeNodeLabel) {
        this.lifeNodeLabel = lifeNodeLabel;
    }

    public LocalDateTime getUnlockAt() {
        return unlockAt;
    }

    public void setUnlockAt(LocalDateTime unlockAt) {
        this.unlockAt = unlockAt;
    }

    public RecordAttachmentVO getCover() {
        return cover;
    }

    public void setCover(RecordAttachmentVO cover) {
        this.cover = cover;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getTagNames() {
        return tagNames;
    }

    public void setTagNames(List<String> tagNames) {
        this.tagNames = tagNames;
    }
}
