package com.flashback.common.exception;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, String message) {
        this(code, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public BizException(int code, HttpStatus httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
