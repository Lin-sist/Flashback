package com.flashback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordTagMapper {

    int deleteByRecordId(@Param("recordId") Long recordId);

    /**
     * C2：读取记录已绑定的 tagId，供 Agent 工具「追加标签」在合并前取现状。
     * 追加语义必须先知道既有标签，否则无法保证不清空。
     */
    List<Long> selectTagIdsByRecordId(@Param("recordId") Long recordId);

    int batchInsert(@Param("recordId") Long recordId, @Param("tagIds") List<Long> tagIds);
}
