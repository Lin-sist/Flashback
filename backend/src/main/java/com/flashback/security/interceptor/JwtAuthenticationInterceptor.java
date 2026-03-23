package com.flashback.security.interceptor;

import com.flashback.common.exception.ForbiddenException;
import com.flashback.common.exception.UnauthorizedException;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一 JWT 鉴权拦截器。
 *
 * 关键行为：
 * 1. 放行 OPTIONS 预检请求；
 * 2. 校验 Token 有效性并解析 AuthUser；
 * 3. /admin 路径强制要求 ADMIN 角色；
 * 4. 将 AuthUser 写入 request attribute 供后续读取。
 */
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_ROOT = "/admin";
    private static final String ADMIN_PATH_PREFIX = "/admin/";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("未登录或 Token 缺失");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenProvider.validate(token)) {
            throw new UnauthorizedException("Token 无效或已过期");
        }

        AuthUser authUser;
        try {
            authUser = jwtTokenProvider.parseAuthUser(token);
        } catch (Exception ex) {
            throw new UnauthorizedException("Token 身份信息不合法");
        }

        if (isAdminPath(request.getRequestURI()) && authUser.getRole() != AuthRole.ADMIN) {
            throw new ForbiddenException("无权限访问后台接口");
        }

        request.setAttribute(AuthUser.REQUEST_ATTRIBUTE, authUser);
        request.setAttribute("jwtClaims", jwtTokenProvider.parseClaims(token));
        return true;
    }

    private boolean isAdminPath(String requestUri) {
        return ADMIN_PATH_ROOT.equals(requestUri) || requestUri.startsWith(ADMIN_PATH_PREFIX);
    }
}
