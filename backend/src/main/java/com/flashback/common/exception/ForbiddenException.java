package com.flashback.common.exception;

import com.flashback.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends BizException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, message);
    }
}
