package com.flashback.domain;

/**
 * 记录列表/详情上附带的最小篇章摘要，仅供安全视图组装。
 */
public class RecordChapterSummaryRow {

    private Long recordId;
    private Long chapterId;
    private String chapterName;
    private TimeChapterStatus chapterStatus;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
    public TimeChapterStatus getChapterStatus() { return chapterStatus; }
    public void setChapterStatus(TimeChapterStatus chapterStatus) { this.chapterStatus = chapterStatus; }
}
