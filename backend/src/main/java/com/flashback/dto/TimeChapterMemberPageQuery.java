package com.flashback.dto;

import com.flashback.common.page.PageQuery;

/**
 * 篇章成员分页查询。order 只接受 ASC / DESC。
 */
public class TimeChapterMemberPageQuery extends PageQuery {

    private String order = "DESC";

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }
}
