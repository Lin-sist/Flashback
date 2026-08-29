package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordChapterSummaryRow;
import com.flashback.domain.TimeChapterRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TimeChapterRecordMapper {

    List<TimeChapterRecord> selectByRecordIdsAndUserId(
            @Param("recordIds") List<Long> recordIds,
            @Param("userId") Long userId);

    TimeChapterRecord selectByRecordIdAndUserIdForUpdate(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    int insert(TimeChapterRecord relation);

    int deleteByChapterIdAndRecordIdAndUserId(
            @Param("chapterId") Long chapterId,
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    long countByChapterIdAndUserId(@Param("chapterId") Long chapterId, @Param("userId") Long userId);

    List<Record> selectMemberRecords(
            @Param("chapterId") Long chapterId,
            @Param("userId") Long userId,
            @Param("order") String order,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    List<Long> selectRecordIdsByChapterIdAndUserId(
            @Param("chapterId") Long chapterId,
            @Param("userId") Long userId);

    List<RecordChapterSummaryRow> selectSummariesByRecordIds(
            @Param("recordIds") List<Long> recordIds,
            @Param("userId") Long userId);

    int deleteAllByUserId(@Param("userId") Long userId);
}
