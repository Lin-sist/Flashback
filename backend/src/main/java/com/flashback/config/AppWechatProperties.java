package com.flashback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WeChat Mini Program integration settings.
 */
@Component
@ConfigurationProperties(prefix = "app.wechat.mini-program")
public class AppWechatProperties {

    private String appId;
    private String secret;
    private String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
    private long timeoutMillis = 5000;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCode2SessionUrl() {
        return code2SessionUrl;
    }

    public void setCode2SessionUrl(String code2SessionUrl) {
        this.code2SessionUrl = code2SessionUrl;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isConfigured() {
        return hasText(appId) && hasText(secret);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
