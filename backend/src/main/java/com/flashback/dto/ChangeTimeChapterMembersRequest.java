package com.flashback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量加入/移出篇章请求。
 */
public class ChangeTimeChapterMembersRequest {

    @NotNull(message = "expectedVersion不能为空")
    private Long expectedVersion;

    @NotEmpty(message = "recordIds不能为空")
    @Size(max = 100, message = "单次最多处理100条记录")
    private List<Long> recordIds;

    @Valid
    private List<TransferConfirmation> transfers;

    public Long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Long expectedVersion) { this.expectedVersion = expectedVersion; }
    public List<Long> getRecordIds() { return recordIds; }
    public void setRecordIds(List<Long> recordIds) { this.recordIds = recordIds; }
    public List<TransferConfirmation> getTransfers() { return transfers; }
    public void setTransfers(List<TransferConfirmation> transfers) { this.transfers = transfers; }
}
