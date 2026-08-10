package com.flashback.service;

import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.StorageProvider;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DraftCleanupWorkerTest {

    @Mock private RecordMapper recordMapper;
    @Mock private RecordAttachmentMapper recordAttachmentMapper;
    @Mock private ObjectStorageRegistry objectStorageRegistry;
    @Mock private ObjectStorageProvider storage;

    private DraftCleanupWorker worker;
    private LocalDateTime expiry;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        worker = new DraftCleanupWorker(recordMapper, recordAttachmentMapper, objectStorageRegistry);
        expiry = LocalDateTime.of(2026, 8, 1, 0, 0);
        now = LocalDateTime.of(2026, 8, 10, 0, 0);
    }

    @Test
    void shouldDeleteExpiredDraftAfterAllRemoteObjectsAreGone() {
        when(recordMapper.selectExpiredDraftForUpdate(10L, 1L, expiry, now)).thenReturn(record());
        RecordAttachment attachment = attachment();
        when(recordAttachmentMapper.selectAllByRecordIdAndUserId(10L, 1L)).thenReturn(List.of(attachment));
        when(objectStorageRegistry.getRequired(StorageProvider.QINIU)).thenReturn(storage);
        when(recordMapper.deleteExpiredDraftByIdAndUserId(10L, 1L, expiry, now)).thenReturn(1);

        assertThat(worker.cleanup(10L, 1L, expiry, now)).isEqualTo(DraftCleanupResult.DELETED);

        verify(storage).deleteObject("private-bucket", "private-key");
    }

    @Test
    void shouldTreatMissingRemoteObjectAsIdempotentSuccess() {
        when(recordMapper.selectExpiredDraftForUpdate(10L, 1L, expiry, now)).thenReturn(record());
        when(recordAttachmentMapper.selectAllByRecordIdAndUserId(10L, 1L)).thenReturn(List.of(attachment()));
        when(objectStorageRegistry.getRequired(StorageProvider.QINIU)).thenReturn(storage);
        org.mockito.Mockito.doThrow(new ObjectStorageException("missing", true))
                .when(storage).deleteObject("private-bucket", "private-key");
        when(recordMapper.deleteExpiredDraftByIdAndUserId(10L, 1L, expiry, now)).thenReturn(1);

        assertThat(worker.cleanup(10L, 1L, expiry, now)).isEqualTo(DraftCleanupResult.DELETED);
    }

    @Test
    void shouldRetainDatabaseAnchorWhenRemoteDeleteFails() {
        when(recordMapper.selectExpiredDraftForUpdate(10L, 1L, expiry, now)).thenReturn(record());
        when(recordAttachmentMapper.selectAllByRecordIdAndUserId(10L, 1L)).thenReturn(List.of(attachment()));
        when(objectStorageRegistry.getRequired(StorageProvider.QINIU)).thenReturn(storage);
        org.mockito.Mockito.doThrow(new ObjectStorageException("unavailable"))
                .when(storage).deleteObject("private-bucket", "private-key");

        assertThat(worker.cleanup(10L, 1L, expiry, now)).isEqualTo(DraftCleanupResult.RETRY);

        verify(recordMapper, never()).deleteExpiredDraftByIdAndUserId(10L, 1L, expiry, now);
    }

    @Test
    void shouldSkipWhenDraftWasRefreshedOrSavedBeforeLock() {
        when(recordMapper.selectExpiredDraftForUpdate(10L, 1L, expiry, now)).thenReturn(null);

        assertThat(worker.cleanup(10L, 1L, expiry, now)).isEqualTo(DraftCleanupResult.SKIPPED);

        verify(recordAttachmentMapper, never()).selectAllByRecordIdAndUserId(10L, 1L);
    }

    private Record record() {
        Record record = new Record();
        record.setId(10L);
        record.setUserId(1L);
        record.setDraftExpiresAt(expiry);
        return record;
    }

    private RecordAttachment attachment() {
        RecordAttachment attachment = new RecordAttachment();
        attachment.setStorageProvider(StorageProvider.QINIU);
        attachment.setBucket("private-bucket");
        attachment.setStorageKey("private-key");
        return attachment;
    }
}
