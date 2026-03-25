package com.flashback.vo;

/**
 * 用户注册返回体。
 */
public class RegisterResponseVO {

    private Long userId;

    public RegisterResponseVO() {
    }

    public RegisterResponseVO(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
