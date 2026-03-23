package com.flashback.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 通用分页请求参数。
 * pageNum 从 1 开始；pageSize 用于限定单页数量。
 *
 * 使用约定：Controller 接收该对象时应配合 @Valid（必要时类上加 @Validated），
 * 以确保分页约束能被统一异常处理捕获。
 */
public class PageQuery {

    @Min(value = 1, message = "pageNum 最小为 1")
    private int pageNum = 1;

    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 200, message = "pageSize 最大为 200")
    private int pageSize = 10;

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
}
