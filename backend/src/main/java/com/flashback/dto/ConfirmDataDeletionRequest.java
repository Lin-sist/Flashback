package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ConfirmDataDeletionRequest {
    @NotNull
    private Long intentId;
    @NotBlank
    private String confirmationText;
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public String getConfirmationText() { return confirmationText; }
    public void setConfirmationText(String confirmationText) { this.confirmationText = confirmationText; }
}
