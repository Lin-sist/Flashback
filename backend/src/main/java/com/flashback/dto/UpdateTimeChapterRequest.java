package com.flashback.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改篇章文案时的乐观锁请求。
 */
public class UpdateTimeChapterRequest {

    @NotNull(message = "expectedVersion不能为空")
    private Long expectedVersion;

    @NotNull(message = "name不能为空")
    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Size(max = 1000, message = "note长度不能超过1000")
    private String note;

    public Long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
