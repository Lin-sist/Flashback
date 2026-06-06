package com.flashback.mapper;

import com.flashback.domain.RecordReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecordReminderMapper {

    RecordReminder selectByRecordIdAndTemplateType(
            @Param("recordId") Long recordId,
            @Param("templateType") String templateType);

    int insert(RecordReminder recordReminder);
}
