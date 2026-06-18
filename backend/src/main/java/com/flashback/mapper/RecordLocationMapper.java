package com.flashback.mapper;

import com.flashback.domain.RecordLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecordLocationMapper {

    RecordLocation selectByRecordIdAndUserId(@Param("recordId") Long recordId, @Param("userId") Long userId);

    int upsert(RecordLocation recordLocation);

    int deleteByRecordIdAndUserId(@Param("recordId") Long recordId, @Param("userId") Long userId);
}
