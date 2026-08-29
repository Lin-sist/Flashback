package com.flashback.vo;

import com.flashback.domain.TimeChapterStatus;

/**
 * 记录上展示的最小篇章信息，不带篇章自述和成员内容。
 */
public class RecordChapterSummaryVO {

    private Long id;
    private String name;
    private TimeChapterStatus status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TimeChapterStatus getStatus() { return status; }
    public void setStatus(TimeChapterStatus status) { this.status = status; }
}
