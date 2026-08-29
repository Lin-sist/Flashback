package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * 用户手动组织的一组时间片段。
 *
 * memberCount / coverage* 是查询投影，不落库；它们始终由关系表和 record.created_at 推导。
 */
public class TimeChapter {

    private Long id;
    private Long userId;
    private String name;
    private String note;
    private TimeChapterStatus status;
    private LocalDateTime endedAt;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer memberCount;
    private LocalDateTime coverageStartAt;
    private LocalDateTime coverageEndAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public TimeChapterStatus getStatus() { return status; }
    public void setStatus(TimeChapterStatus status) { this.status = status; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public LocalDateTime getCoverageStartAt() { return coverageStartAt; }
    public void setCoverageStartAt(LocalDateTime coverageStartAt) { this.coverageStartAt = coverageStartAt; }
    public LocalDateTime getCoverageEndAt() { return coverageEndAt; }
    public void setCoverageEndAt(LocalDateTime coverageEndAt) { this.coverageEndAt = coverageEndAt; }
}
