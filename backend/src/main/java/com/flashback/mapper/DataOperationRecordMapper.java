package com.flashback.mapper;

import com.flashback.domain.DataOperationItemStatus;
import com.flashback.domain.DataOperationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataOperationRecordMapper {
    int insert(DataOperationRecord item);
    List<DataOperationRecord> selectByOperationIdAndUserId(
            @Param("operationId") Long operationId,
            @Param("userId") Long userId);
    int updateIfStatus(
            @Param("item") DataOperationRecord item,
            @Param("expectedStatus") DataOperationItemStatus expectedStatus);
    int countActiveByUserAndRecord(@Param("userId") Long userId, @Param("recordId") Long recordId);
}
