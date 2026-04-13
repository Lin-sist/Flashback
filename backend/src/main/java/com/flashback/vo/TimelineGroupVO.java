package com.flashback.vo;

import java.util.List;

/**
 * 时间轴按年月分组视图。
 */
public class TimelineGroupVO {

    private String yearMonth;
    private List<TimelineItemVO> items;

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public List<TimelineItemVO> getItems() {
        return items;
    }

    public void setItems(List<TimelineItemVO> items) {
        this.items = items;
    }
}
