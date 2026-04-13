package com.flashback.dto;

import com.flashback.common.page.PageQuery;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;

/**
 * 我的记录列表分页查询。
 */
public class RecordPageQuery extends PageQuery {

    private RecordStatus status;
    private RecordType recordType;
    private Long tagId;
    private String keyword;

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

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
