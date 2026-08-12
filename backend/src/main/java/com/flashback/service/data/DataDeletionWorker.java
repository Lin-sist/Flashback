package com.flashback.service.data;

import com.flashback.domain.DataOperationFailureCode;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.DataOwnershipInvariantMapper;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class DataDeletionWorker {
    private final RecordMapper recordMapper;
    private final RecordAttachmentMapper attachmentMapper;
    private final ObjectStorageRegistry storageRegistry;
    private final DataOwnershipInvariantMapper invariantMapper;
    public DataDeletionWorker(RecordMapper recordMapper, RecordAttachmentMapper attachmentMapper, ObjectStorageRegistry storageRegistry,
            DataOwnershipInvariantMapper invariantMapper) {
        this.recordMapper = recordMapper; this.attachmentMapper = attachmentMapper; this.storageRegistry = storageRegistry; this.invariantMapper = invariantMapper;
    }

    @Transactional
    public Result deleteRecord(Long userId, Long recordId) {
        Record record = recordMapper.selectByIdAndUserIdForDeletion(recordId, userId);
        if (record == null) return Result.success();
        if (invariantMapper.countRecordLinkedOwnerMismatches(recordId, userId) > 0) {
            return Result.failed(DataOperationFailureCode.DERIVED_DATA_REMAINS);
        }
        List<RecordAttachment> attachments = attachmentMapper.selectAllByRecordIdAndUserId(recordId, userId);
        for (RecordAttachment attachment : attachments) {
            try {
                storageRegistry.getRequired(attachment.getStorageProvider()).deleteObject(attachment.getBucket(), attachment.getStorageKey());
            } catch (ObjectStorageException ex) {
                if (!ex.isNotFound()) return Result.retry(DataOperationFailureCode.REMOTE_OBJECT_DELETE_FAILED);
            } catch (RuntimeException ex) {
                return Result.retry(DataOperationFailureCode.REMOTE_OBJECT_DELETE_FAILED);
            }
        }
        if (recordMapper.deleteAnyByIdAndUserId(recordId, userId) != 1) return Result.retry(DataOperationFailureCode.DATABASE_DELETE_FAILED);
        return Result.success();
    }
    public record Result(boolean succeeded, boolean retryable, DataOperationFailureCode failureCode) {
        static Result success() { return new Result(true, false, null); }
        static Result retry(DataOperationFailureCode code) { return new Result(false, true, code); }
        static Result failed(DataOperationFailureCode code) { return new Result(false, false, code); }
    }
}
