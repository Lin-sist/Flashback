package com.flashback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataOwnershipInvariantMapper {
    int countRecordLinkedOwnerMismatches(@Param("recordId") Long recordId, @Param("userId") Long userId);
}
