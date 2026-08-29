package com.flashback.service.data;

import com.flashback.domain.TimeChapterStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出包中的篇章元数据快照。故意只包含成员 record ID，不带记录正文。
 */
public record DataExportChapterSnapshot(
        Long id,
        String name,
        String note,
        TimeChapterStatus status,
        Integer memberCount,
        LocalDateTime coverageStartAt,
        LocalDateTime coverageEndAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Long> memberRecordIds) {
}
