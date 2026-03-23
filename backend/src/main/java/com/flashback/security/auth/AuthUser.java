package com.flashback.security.auth;

/**
 * 登录态用户最小身份模型。
 *
 * 约定：由 JwtAuthenticationInterceptor 解析后放入 request attribute，
 * key 为 AuthUser.REQUEST_ATTRIBUTE，供参数解析器和公共工具复用。
 */
public class AuthUser {

    public static final String REQUEST_ATTRIBUTE = "authUser";

    private final Long userId;
    private final AuthRole role;

    public AuthUser(Long userId, AuthRole role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public AuthRole getRole() {
        return role;
    }
}
