package com.flashback.security.auth;

import com.flashback.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将 @CurrentUser AuthUser 参数解析为拦截器写入 request attribute 的登录身份。
 *
 * 关键约定：JwtAuthenticationInterceptor 必须先执行并写入 AuthUser.REQUEST_ATTRIBUTE。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AuthUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new UnauthorizedException("无法获取当前登录用户");
        }
        Object authUser = request.getAttribute(AuthUser.REQUEST_ATTRIBUTE);
        if (!(authUser instanceof AuthUser)) {
            throw new UnauthorizedException("登录信息不存在或已失效");
        }
        return authUser;
    }
}
