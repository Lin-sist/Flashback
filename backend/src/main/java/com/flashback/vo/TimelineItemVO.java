package com.flashback.vo;

import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 时间轴记录项视图。
 */
public class TimelineItemVO {

    private Long id;
    private String title;
    private RecordStatus status;
    private RecordType recordType;
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

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
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
