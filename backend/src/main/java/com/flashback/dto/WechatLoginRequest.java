package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * WeChat Mini Program code login request.
 */
public class WechatLoginRequest {

    @NotBlank(message = "code不能为空")
    @Size(max = 512, message = "code长度不能超过512")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
