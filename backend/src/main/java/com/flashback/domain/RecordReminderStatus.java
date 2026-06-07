package com.flashback.domain;

/**
 * Record unlock reminder states accepted by M3.
 */
public enum RecordReminderStatus {
    REQUESTED,
    AUTHORIZED,
    DENIED,
    NOT_CONFIGURED,
    SEND_PENDING,
    SEND_SUCCESS,
    SEND_FAILED,
    SKIPPED_NO_OPENID
}
