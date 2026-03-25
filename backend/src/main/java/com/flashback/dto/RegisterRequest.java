package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求。
 */
public class RegisterRequest {

    @NotBlank(message = "username不能为空")
    @Size(min = 3, max = 50, message = "username长度需在3-50之间")
    private String username;

    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 64, message = "password长度需在6-64之间")
    private String password;

    @NotBlank(message = "nickname不能为空")
    @Size(max = 50, message = "nickname长度不能超过50")
    private String nickname;

    @Size(max = 100, message = "email长度不能超过100")
    private String email;

    @Size(max = 255, message = "avatar长度不能超过255")
    private String avatar;

    @Size(max = 100, message = "openid长度不能超过100")
    private String openid;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }
}
