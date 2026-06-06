package com.flashback.domain;

/**
 * 记录解锁提醒的最小状态。
 */
public enum RecordReminderStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED_NO_OPENID
}
