package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RecordMapper {

    int insert(Record record);

    Record selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateDraftByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("content") String content,
            @Param("recordType") RecordType recordType,
            @Param("coreQuestion") String coreQuestion,
            @Param("unlockAt") LocalDateTime unlockAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    int deleteDraftByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int sealDraftByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("sealedAt") LocalDateTime sealedAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    List<Record> selectExpiredSealedRecords(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    int unlockSealedById(
            @Param("id") Long id,
            @Param("unlockedAt") LocalDateTime unlockedAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    long countByUserAndCondition(
            @Param("userId") Long userId,
            @Param("status") RecordStatus status,
            @Param("recordType") RecordType recordType);

    List<Record> selectPageByUserAndCondition(
            @Param("userId") Long userId,
            @Param("status") RecordStatus status,
            @Param("recordType") RecordType recordType,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countUnlockedByUser(@Param("userId") Long userId);

    List<Record> selectUnlockedPageByUser(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);
}
