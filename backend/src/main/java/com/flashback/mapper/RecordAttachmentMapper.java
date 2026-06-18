package com.flashback.mapper;

import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordAttachmentMapper {

    int insert(RecordAttachment attachment);

    RecordAttachment selectByIdAndRecordIdAndUserId(
            @Param("id") Long id,
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    List<RecordAttachment> selectAvailableByRecordIdAndUserId(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    int countAvailableByRecordIdAndUserIdAndType(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId,
            @Param("type") RecordAttachmentType type);

    int countAvailableByRecordIdAndUserId(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    Long sumAvailableSizeByRecordIdAndUserId(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    int markDeletedByIdAndRecordIdAndUserId(
            @Param("id") Long id,
            @Param("recordId") Long recordId,
            @Param("userId") Long userId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
