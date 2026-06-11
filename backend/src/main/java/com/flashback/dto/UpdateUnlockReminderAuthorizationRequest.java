package com.flashback.dto;

import com.flashback.domain.RecordReminderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Unlock reminder authorization result reported by Mini Program seal flow.
 */
public class UpdateUnlockReminderAuthorizationRequest {

    @NotNull(message = "status不能为空")
    private RecordReminderStatus status;

    public RecordReminderStatus getStatus() {
        return status;
    }

    public void setStatus(RecordReminderStatus status) {
        this.status = status;
    }
}
