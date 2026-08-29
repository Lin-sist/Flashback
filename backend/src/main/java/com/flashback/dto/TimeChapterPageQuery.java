package com.flashback.dto;

import com.flashback.common.page.PageQuery;
import com.flashback.domain.TimeChapterStatus;

/**
 * 篇章摘要分页查询。
 */
public class TimeChapterPageQuery extends PageQuery {

    private TimeChapterStatus status;

    public TimeChapterStatus getStatus() { return status; }
    public void setStatus(TimeChapterStatus status) { this.status = status; }
}
