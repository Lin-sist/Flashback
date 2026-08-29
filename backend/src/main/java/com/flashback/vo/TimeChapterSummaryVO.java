package com.flashback.vo;

import com.flashback.domain.TimeChapterStatus;

import java.time.LocalDateTime;

/**
 * 篇章摘要视图。
 */
public class TimeChapterSummaryVO {

    private Long id;
    private String name;
    private String note;
    private TimeChapterStatus status;
    private Integer memberCount;
    private LocalDateTime coverageStartAt;
    private LocalDateTime coverageEndAt;
    private LocalDateTime endedAt;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public TimeChapterStatus getStatus() { return status; }
    public void setStatus(TimeChapterStatus status) { this.status = status; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public LocalDateTime getCoverageStartAt() { return coverageStartAt; }
    public void setCoverageStartAt(LocalDateTime coverageStartAt) { this.coverageStartAt = coverageStartAt; }
    public LocalDateTime getCoverageEndAt() { return coverageEndAt; }
    public void setCoverageEndAt(LocalDateTime coverageEndAt) { this.coverageEndAt = coverageEndAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
