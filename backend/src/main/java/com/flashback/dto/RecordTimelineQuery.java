package com.flashback.dto;

import com.flashback.common.page.PageQuery;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * 时间轴查询条件。
 */
public class RecordTimelineQuery extends PageQuery {

    private Long tagId;

    @Min(value = 1970, message = "year最小为1970")
    @Max(value = 9999, message = "year最大为9999")
    private Integer year;

    @Min(value = 1, message = "month最小为1")
    @Max(value = 12, message = "month最大为12")
    private Integer month;

    @Min(value = 1, message = "day最小为1")
    @Max(value = 31, message = "day最大为31")
    private Integer day;

    public RecordTimelineQuery() {
        setPageSize(20);
    }

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

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    @Override
    @Max(value = 50, message = "pageSize 最大为 50")
    public int getPageSize() {
        return super.getPageSize();
    }

    @AssertTrue(message = "年月日筛选条件无效")
    public boolean isDateFilterValid() {
        if (month != null && year == null) {
            return false;
        }
        if (day != null && (year == null || month == null)) {
            return false;
        }
        if (day == null) {
            return true;
        }
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }
}
