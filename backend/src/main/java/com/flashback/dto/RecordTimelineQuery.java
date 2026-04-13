package com.flashback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 时间轴查询条件。
 */
public class RecordTimelineQuery {

    private Long tagId;

    @Min(value = 1970, message = "year最小为1970")
    @Max(value = 9999, message = "year最大为9999")
    private Integer year;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
