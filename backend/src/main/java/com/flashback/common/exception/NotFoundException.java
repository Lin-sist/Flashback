package com.flashback.common.exception;

import com.flashback.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public class NotFoundException extends BizException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
