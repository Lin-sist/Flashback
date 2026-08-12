package com.flashback.service.data;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.mapper.DataOperationMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DataOwnershipMutationGuard {
    private final DataOperationMapper operationMapper;
    public DataOwnershipMutationGuard(DataOperationMapper operationMapper) { this.operationMapper = operationMapper; }
    public void assertWritable(Long userId) {
        if (operationMapper.countBlockingClearAllByUser(userId) > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, "清除全部记录正在进行，暂时不能修改记录");
        }
    }
}
