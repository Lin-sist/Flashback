package com.flashback.domain;

public enum DataOperationStatus {
    PREPARED,
    PENDING,
    RUNNING,
    RETRY_REQUIRED,
    SUCCEEDED,
    FAILED,
    EXPIRED
}
