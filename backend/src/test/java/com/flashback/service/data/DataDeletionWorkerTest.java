package com.flashback.service.data;

import com.flashback.domain.*;
import com.flashback.domain.Record;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.DataOwnershipInvariantMapper;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataDeletionWorkerTest {
    private final RecordMapper records = mock(RecordMapper.class);
    private final RecordAttachmentMapper attachments = mock(RecordAttachmentMapper.class);
    private final ObjectStorageRegistry registry = mock(ObjectStorageRegistry.class);
    private final ObjectStorageProvider provider = mock(ObjectStorageProvider.class);
    private final DataOwnershipInvariantMapper invariants = mock(DataOwnershipInvariantMapper.class);
    private final DataDeletionWorker worker = new DataDeletionWorker(records, attachments, registry, invariants);

    @Test
    void providerNotFoundIsIdempotentAndDatabaseDeleteContinues() {
        Record record = record(); RecordAttachment attachment = attachment();
        when(records.selectByIdAndUserIdForDeletion(2L, 1L)).thenReturn(record);
        when(attachments.selectAllByRecordIdAndUserId(2L, 1L)).thenReturn(List.of(attachment));
        when(registry.getRequired(StorageProvider.QINIU)).thenReturn(provider);
        doThrow(new ObjectStorageException("not found", true)).when(provider).deleteObject("bucket", "key");
        when(records.deleteAnyByIdAndUserId(2L, 1L)).thenReturn(1);
        assertTrue(worker.deleteRecord(1L, 2L).succeeded());
        verify(records).deleteAnyByIdAndUserId(2L, 1L);
    }

    @Test
    void providerFailureRetainsDatabaseRecordForRetry() {
        when(records.selectByIdAndUserIdForDeletion(2L, 1L)).thenReturn(record());
        when(attachments.selectAllByRecordIdAndUserId(2L, 1L)).thenReturn(List.of(attachment()));
        when(registry.getRequired(StorageProvider.QINIU)).thenReturn(provider);
        doThrow(new ObjectStorageException("temporary")).when(provider).deleteObject("bucket", "key");
        DataDeletionWorker.Result result = worker.deleteRecord(1L, 2L);
        assertFalse(result.succeeded()); assertTrue(result.retryable()); assertEquals(DataOperationFailureCode.REMOTE_OBJECT_DELETE_FAILED, result.failureCode());
        verify(records, never()).deleteAnyByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void ownerMismatchFailsClosedBeforeRemoteOrDatabaseDelete() {
        when(records.selectByIdAndUserIdForDeletion(2L, 1L)).thenReturn(record());
        when(invariants.countRecordLinkedOwnerMismatches(2L, 1L)).thenReturn(1);
        DataDeletionWorker.Result result = worker.deleteRecord(1L, 2L);
        assertFalse(result.succeeded()); assertFalse(result.retryable()); assertEquals(DataOperationFailureCode.DERIVED_DATA_REMAINS, result.failureCode());
        verifyNoInteractions(registry); verify(records, never()).deleteAnyByIdAndUserId(anyLong(), anyLong());
    }

    private Record record() { Record r = new Record(); r.setId(2L); r.setUserId(1L); r.setStatus(RecordStatus.SAVED); return r; }
    private RecordAttachment attachment() { RecordAttachment a = new RecordAttachment(); a.setId(3L); a.setRecordId(2L); a.setUserId(1L); a.setStorageProvider(StorageProvider.QINIU); a.setBucket("bucket"); a.setStorageKey("key"); return a; }
}
