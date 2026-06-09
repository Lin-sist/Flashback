package com.flashback.mapper;

import com.flashback.domain.RecordReminder;
import com.flashback.domain.RecordReminderStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RecordReminderMapper {

    RecordReminder selectByRecordIdAndTemplateType(
            @Param("recordId") Long recordId,
            @Param("templateType") String templateType);

    int insert(RecordReminder recordReminder);

    int updateStatusById(
            @Param("id") Long id,
            @Param("reminderStatus") RecordReminderStatus reminderStatus,
            @Param("lastError") String lastError,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("updatedAt") LocalDateTime updatedAt);
}
