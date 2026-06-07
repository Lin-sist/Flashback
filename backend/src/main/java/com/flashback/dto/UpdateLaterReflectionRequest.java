package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * M3 later reflection update request.
 */
public class UpdateLaterReflectionRequest {

    @NotBlank(message = "realityLater不能为空")
    @Size(max = 2000, message = "realityLater长度不能超过2000")
    private String realityLater;

    public String getRealityLater() {
        return realityLater;
    }

    public void setRealityLater(String realityLater) {
        this.realityLater = realityLater;
    }
}
