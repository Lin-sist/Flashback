package com.flashback.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建时间篇章并一次性加入记录。
 */
public class CreateTimeChapterRequest {

    @NotBlank(message = "name不能为空")
    @Size(max = 100, message = "name长度不能超过100")
    private String name;

    @Size(max = 1000, message = "note长度不能超过1000")
    private String note;

    @NotEmpty(message = "recordIds不能为空")
    @Size(max = 100, message = "单次最多处理100条记录")
    private List<Long> recordIds;

    @Valid
    private List<TransferConfirmation> transfers;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<Long> getRecordIds() { return recordIds; }
    public void setRecordIds(List<Long> recordIds) { this.recordIds = recordIds; }
    public List<TransferConfirmation> getTransfers() { return transfers; }
    public void setTransfers(List<TransferConfirmation> transfers) { this.transfers = transfers; }
}
