package com.flashback.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 更新当前用户基础资料请求。
 */
public class UpdateUserProfileRequest {

    @Size(max = 50, message = "nickname长度不能超过50")
    private String nickname;

    @Email(message = "email格式不正确")
    @Size(max = 100, message = "email长度不能超过100")
    private String email;

    @Size(max = 255, message = "avatar长度不能超过255")
    private String avatar;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
