package com.flashback.mapper;

import com.flashback.domain.TimeChapter;
import com.flashback.domain.TimeChapterStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TimeChapterMapper {

    int insert(TimeChapter chapter);

    TimeChapter selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    TimeChapter selectByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserAndStatus(@Param("userId") Long userId, @Param("status") TimeChapterStatus status);

    List<TimeChapter> selectPageByUserAndStatus(
            @Param("userId") Long userId,
            @Param("status") TimeChapterStatus status,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    List<TimeChapter> selectAllByUserId(@Param("userId") Long userId);

    int updateMetadataIfVersion(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("note") String note,
            @Param("expectedVersion") Long expectedVersion,
            @Param("updatedAt") LocalDateTime updatedAt);

    int updateLifecycleIfVersion(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") TimeChapterStatus status,
            @Param("endedAt") LocalDateTime endedAt,
            @Param("expectedVersion") Long expectedVersion,
            @Param("updatedAt") LocalDateTime updatedAt);

    int bumpVersion(@Param("id") Long id, @Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);

    int deleteByIdAndUserIdAndVersion(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("expectedVersion") Long expectedVersion);

    int deleteAllByUserId(@Param("userId") Long userId);
}
