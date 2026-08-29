package com.flashback.domain;

import java.time.LocalDateTime;

/**
 * 篇章与记录的主归属关系。
 */
public class TimeChapterRecord {

    private Long chapterId;
    private Long recordId;
    private Long userId;
    private LocalDateTime addedAt;

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
