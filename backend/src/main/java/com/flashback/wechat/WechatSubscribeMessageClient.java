package com.flashback.wechat;

import java.time.LocalDateTime;

public interface WechatSubscribeMessageClient {

    void sendUnlockReminder(String openid, Long recordId, LocalDateTime unlockedAt);
}
