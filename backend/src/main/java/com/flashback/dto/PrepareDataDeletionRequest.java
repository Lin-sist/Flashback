package com.flashback.dto;

import com.flashback.domain.DataDeletionScope;
import jakarta.validation.constraints.NotNull;

public class PrepareDataDeletionRequest {
    @NotNull
    private DataDeletionScope scope;
    private Long recordId;
    public DataDeletionScope getScope() { return scope; }
    public void setScope(DataDeletionScope scope) { this.scope = scope; }
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
}
