package com.flashback.service;

import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 在记录行锁内清理一条过期技术草稿，避免与 refresh/save 竞态误删。
 */
@Service
public class DraftCleanupWorker {

    private final RecordMapper recordMapper;
    private final RecordAttachmentMapper recordAttachmentMapper;
    private final ObjectStorageRegistry objectStorageRegistry;

    public DraftCleanupWorker(
            RecordMapper recordMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            ObjectStorageRegistry objectStorageRegistry) {
        this.recordMapper = recordMapper;
        this.recordAttachmentMapper = recordAttachmentMapper;
        this.objectStorageRegistry = objectStorageRegistry;
    }

    @Transactional
    public DraftCleanupResult cleanup(
            Long recordId,
            Long userId,
            LocalDateTime expectedExpiresAt,
            LocalDateTime now) {
        Record locked = recordMapper.selectExpiredDraftForUpdate(
                recordId,
                userId,
                expectedExpiresAt,
                now);
        if (locked == null) {
            return DraftCleanupResult.SKIPPED;
        }

        List<RecordAttachment> attachments = recordAttachmentMapper.selectAllByRecordIdAndUserId(recordId, userId);
        for (RecordAttachment attachment : attachments == null ? List.<RecordAttachment>of() : attachments) {
            ObjectStorageProvider storage;
            try {
                storage = objectStorageRegistry.getRequired(attachment.getStorageProvider());
                storage.deleteObject(attachment.getBucket(), attachment.getStorageKey());
            } catch (ObjectStorageException ex) {
                if (!ex.isNotFound()) {
                    return DraftCleanupResult.RETRY;
                }
            }
        }

        int deleted = recordMapper.deleteExpiredDraftByIdAndUserId(
                recordId,
                userId,
                expectedExpiresAt,
                now);
        return deleted == 1 ? DraftCleanupResult.DELETED : DraftCleanupResult.SKIPPED;
    }
}
