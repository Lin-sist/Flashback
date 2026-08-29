package com.flashback.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 显式转移确认，避免服务端猜测来源篇章。
 */
public class TransferConfirmation {

    @NotNull(message = "transfers.recordId不能为空")
    private Long recordId;

    @NotNull(message = "transfers.fromChapterId不能为空")
    private Long fromChapterId;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getFromChapterId() { return fromChapterId; }
    public void setFromChapterId(Long fromChapterId) { this.fromChapterId = fromChapterId; }
}
