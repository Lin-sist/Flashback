package com.flashback.vo;

import com.flashback.common.page.PageResult;

/**
 * 篇章摘要与成员分页。
 */
public class TimeChapterDetailVO extends TimeChapterSummaryVO {

    private PageResult<RecordListItemVO> members;

    public PageResult<RecordListItemVO> getMembers() { return members; }
    public void setMembers(PageResult<RecordListItemVO> members) { this.members = members; }
}
