package com.flashback.common.exception;

import com.flashback.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BizException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }
}
