package com.flashback.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.config.AppWechatProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Minimal WeChat code2session client for Mini Program login.
 */
@Component
public class WechatCode2SessionClient implements WechatSessionClient {

    private final AppWechatProperties properties;
    private final ObjectMapper objectMapper;

    public WechatCode2SessionClient(AppWechatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatSession exchangeCodeForSession(String code) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getCode2SessionUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unavailable("微信登录服务暂不可用");
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
                throw unavailable("微信登录校验失败");
            }
            String openid = text(root, "openid");
            if (openid == null) {
                throw unavailable("微信登录未返回有效身份");
            }
            return new WechatSession(openid, text(root, "session_key"));
        } catch (IOException ex) {
            throw unavailable("微信登录服务暂不可用");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable("微信登录服务暂不可用");
        }
    }

    private String text(JsonNode root, String field) {
        if (!root.hasNonNull(field)) {
            return null;
        }
        String value = root.get(field).asText();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private BizException unavailable(String message) {
        return new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
