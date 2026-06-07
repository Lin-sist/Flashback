package com.flashback.wechat;

public interface WechatSessionClient {

    WechatSession exchangeCodeForSession(String code);
}
