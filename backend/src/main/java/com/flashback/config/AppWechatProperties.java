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
    private String accessTokenUrl = "https://api.weixin.qq.com/cgi-bin/token";
    private String subscribeMessageSendUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private long timeoutMillis = 5000;
    private String unlockReminderTemplateId;
    private String unlockReminderPage = "pages/record-detail/index";
    private String unlockReminderThingKey = "thing1";
    private String unlockReminderTimeKey = "time2";

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

    public String getAccessTokenUrl() {
        return accessTokenUrl;
    }

    public void setAccessTokenUrl(String accessTokenUrl) {
        this.accessTokenUrl = accessTokenUrl;
    }

    public String getSubscribeMessageSendUrl() {
        return subscribeMessageSendUrl;
    }

    public void setSubscribeMessageSendUrl(String subscribeMessageSendUrl) {
        this.subscribeMessageSendUrl = subscribeMessageSendUrl;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public String getUnlockReminderTemplateId() {
        return unlockReminderTemplateId;
    }

    public void setUnlockReminderTemplateId(String unlockReminderTemplateId) {
        this.unlockReminderTemplateId = unlockReminderTemplateId;
    }

    public String getUnlockReminderPage() {
        return unlockReminderPage;
    }

    public void setUnlockReminderPage(String unlockReminderPage) {
        this.unlockReminderPage = unlockReminderPage;
    }

    public String getUnlockReminderThingKey() {
        return unlockReminderThingKey;
    }

    public void setUnlockReminderThingKey(String unlockReminderThingKey) {
        this.unlockReminderThingKey = unlockReminderThingKey;
    }

    public String getUnlockReminderTimeKey() {
        return unlockReminderTimeKey;
    }

    public void setUnlockReminderTimeKey(String unlockReminderTimeKey) {
        this.unlockReminderTimeKey = unlockReminderTimeKey;
    }

    public boolean isConfigured() {
        return hasText(appId) && hasText(secret);
    }

    public boolean hasUnlockReminderTemplate() {
        return hasText(unlockReminderTemplateId);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
