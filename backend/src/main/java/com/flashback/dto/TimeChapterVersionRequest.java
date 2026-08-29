package com.flashback.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 生命周期和删除命令的乐观锁请求。
 */
public class TimeChapterVersionRequest {

    @NotNull(message = "expectedVersion不能为空")
    @Min(value = 0, message = "expectedVersion不能小于0")
    private Long expectedVersion;

    public Long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }
}
