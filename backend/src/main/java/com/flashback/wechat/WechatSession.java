package com.flashback.wechat;

/**
 * Trusted identity returned by WeChat code2session.
 */
public class WechatSession {

    private final String openid;
    private final String sessionKey;

    public WechatSession(String openid, String sessionKey) {
        this.openid = openid;
        this.sessionKey = sessionKey;
    }

    public String getOpenid() {
        return openid;
    }

    public String getSessionKey() {
        return sessionKey;
    }
}
