package com.flashback.config;

import com.flashback.security.auth.CurrentUserArgumentResolver;
import com.flashback.security.interceptor.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC 基础配置：统一注册鉴权拦截器、CORS 规则与公共参数解析器。
 *
 * 关键约定：
 * 1. /api 与 /admin 路径进入统一 JWT 鉴权链路；
 * 2. /api/auth/** 与 /admin/auth/** 白名单放行；
 * 3. Controller 可通过 @CurrentUser 直接拿到登录身份。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final AppCorsProperties appCorsProperties;

    public WebMvcConfig(
            JwtAuthenticationInterceptor jwtAuthenticationInterceptor,
            CurrentUserArgumentResolver currentUserArgumentResolver,
            AppCorsProperties appCorsProperties) {
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.appCorsProperties = appCorsProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api", "/api/**", "/admin", "/admin/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/admin/auth/**",
                        "/error",
                        "/actuator/health");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(appCorsProperties.getAllowedOriginPatterns().toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
