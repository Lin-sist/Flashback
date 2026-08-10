package com.flashback.service;

import com.flashback.domain.Record;
import com.flashback.mapper.RecordAttachmentMapper;
import org.springframework.stereotype.Component;

/**
 * P3.1 保存成立条件的单一权威：非空正文或至少一个已确认可用的图片/声音附件。
 */
@Component
public class RecordSaveEligibility {

    private final RecordAttachmentMapper recordAttachmentMapper;

    public RecordSaveEligibility(RecordAttachmentMapper recordAttachmentMapper) {
        this.recordAttachmentMapper = recordAttachmentMapper;
    }

    public boolean isEligible(Record record) {
        return isEligible(record, record == null ? null : record.getContent());
    }

    public boolean isEligible(Record record, String candidateContent) {
        if (record == null) {
            return false;
        }
        if (candidateContent != null && !candidateContent.trim().isEmpty()) {
            return true;
        }
        return recordAttachmentMapper.countAvailableByRecordIdAndUserId(
                record.getId(),
                record.getUserId()) > 0;
    }

    public boolean isEligibleAfterRemovingAttachment(Record record) {
        if (record == null) {
            return false;
        }
        if (record.getContent() != null && !record.getContent().trim().isEmpty()) {
            return true;
        }
        return recordAttachmentMapper.countAvailableByRecordIdAndUserId(
                record.getId(),
                record.getUserId()) > 1;
    }
}
