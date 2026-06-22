package com.flashback.vo;

import java.util.List;

/**
 * 时间轴记录分页视图；分页元数据按记录计数，groups 仅负责当前页的年月分组。
 */
public class TimelinePageVO {

    private List<TimelineGroupVO> groups;
    private long total;
    private int pageNum;
    private int pageSize;
    private boolean hasMore;

    public static TimelinePageVO of(
            List<TimelineGroupVO> groups,
            long total,
            int pageNum,
            int pageSize) {
        TimelinePageVO vo = new TimelinePageVO();
        vo.setGroups(groups);
        vo.setTotal(total);
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        vo.setHasMore((long) pageNum * pageSize < total);
        return vo;
    }

    public List<TimelineGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(List<TimelineGroupVO> groups) {
        this.groups = groups;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
