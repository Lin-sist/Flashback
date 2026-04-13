package com.flashback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordTagMapper {

    int deleteByRecordId(@Param("recordId") Long recordId);

    int batchInsert(@Param("recordId") Long recordId, @Param("tagIds") List<Long> tagIds);
}
