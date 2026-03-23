package com.flashback.common.error;

public final class ErrorCode {

    private ErrorCode() {
    }

    public static final int BAD_REQUEST = 40000;
    public static final int UNAUTHORIZED = 40100;
    public static final int FORBIDDEN = 40300;
    public static final int NOT_FOUND = 40400;
    public static final int INTERNAL_ERROR = 50000;
}
