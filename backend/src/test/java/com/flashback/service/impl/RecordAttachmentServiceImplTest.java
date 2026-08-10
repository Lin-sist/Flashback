package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppMediaProperties;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.StorageProvider;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.RecordSaveEligibility;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageMetadata;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import com.flashback.storage.ObjectStorageUploadAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAttachmentServiceImplTest {

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private RecordAttachmentMapper recordAttachmentMapper;

    private AppStorageProperties storageProperties;
    private AppMediaProperties mediaProperties;
    private StubObjectStorageProvider storageProvider;
    private RecordAttachmentServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-18T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        storageProperties = new AppStorageProperties();
        storageProperties.getQiniu().setAccessKey("test-ak");
        storageProperties.getQiniu().setSecretKey("test-sk");
        storageProperties.getQiniu().setBucket("flashback-private");
        storageProperties.getQiniu().setPrivateDomain("https://media.example.com");
        mediaProperties = new AppMediaProperties();
        storageProvider = new StubObjectStorageProvider();
        ObjectStorageRegistry storageRegistry = new ObjectStorageRegistry(storageProperties, List.of(storageProvider));
        service = new RecordAttachmentServiceImpl(
                recordMapper,
                recordAttachmentMapper,
                mediaProperties,
                storageRegistry,
                new RecordSaveEligibility(recordAttachmentMapper),
                clock,
                () -> UUID.fromString("11111111-1111-1111-1111-111111111111"));
        lenient().when(recordMapper.touchDraftByIdAndUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);
    }

    @Test
    void shouldCreateScopedQiniuUploadTokenForDraftImage() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(2);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(1024L);

        CreateAttachmentUploadTokenRequest request = request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L);

        var result = service.createUploadToken(1L, 10L, request);

        assertThat(result.getProvider()).isEqualTo("QINIU");
        assertThat(result.getBucket()).isEqualTo("flashback-private");
        assertThat(result.getKey())
                .isEqualTo("flashback/users/1/records/10/image/11111111-1111-1111-1111-111111111111.jpg");
        assertThat(result.getUploadUrl()).isEqualTo("https://upload.example.com");
        assertThat(result.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 10, 10, 0));
        assertThat(result.getMaxFileSizeBytes()).isEqualTo(41943040L);
        assertThat(result.getUploadMethod()).isEqualTo("POST_MULTIPART");
        assertThat(result.getUploadFormData()).containsEntry("token", "test-upload-token");
    }

    @Test
    void shouldCreateUploadTokenForSavedRecord() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.SAVED));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(0);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(0L);

        var result = service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L));

        assertThat(result.getProvider()).isEqualTo("QINIU");
    }

    @Test
    void shouldRejectWhenRecordNotOwned() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
    }

    @Test
    void shouldRejectWhenRecordIsNotDraft() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.SEALED));

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L)))
                .isInstanceOf(BizException.class)
                .hasMessage("记录已封存，不能修改附件");
        verify(recordAttachmentMapper, never()).countAvailableByRecordIdAndUserIdAndType(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnServiceUnavailableWhenQiniuIsNotConfigured() {
        storageProvider.configured = false;
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L)))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("存储服务未配置");
                });
    }

    @Test
    void shouldRejectUnsupportedMimeType() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.gif", "image/gif", 123456L)))
                .isInstanceOf(BizException.class)
                .hasMessage("图片类型不支持");
    }

    @Test
    void shouldRejectFileLargerThanLimit() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.VOICE, "voice.mp3", "audio/mpeg", 41943041L)))
                .isInstanceOf(BizException.class)
                .hasMessage("单个附件不能超过40MB");
    }

    @Test
    void shouldRejectWhenImageCountLimitExceeded() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(9);

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.IMAGE, "example.jpg", "image/jpeg", 123456L)))
                .isInstanceOf(BizException.class)
                .hasMessage("图片数量不能超过9张");
    }

    @Test
    void shouldRejectWhenTotalSizeLimitExceeded() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.VOICE))
                .thenReturn(1);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(314572000L);

        assertThatThrownBy(() -> service.createUploadToken(
                1L,
                10L,
                request(RecordAttachmentType.VOICE, "voice.mp3", "audio/mpeg", 1000L)))
                .isInstanceOf(BizException.class)
                .hasMessage("单条记录附件总大小不能超过300MB");
    }

    @Test
    void shouldCommitAttachmentAfterQiniuStatVerification() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(2);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(1024L);
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserId(10L, 1L)).thenReturn(3);
        when(recordAttachmentMapper.insert(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.flashback.domain.RecordAttachment attachment = invocation.getArgument(0);
            attachment.setId(99L);
            return 1;
        });
        ObjectStorageMetadata metadata = new ObjectStorageMetadata();
        metadata.setSizeBytes(123456L);
        metadata.setMimeType("image/jpeg");
        storageProvider.metadata = metadata;

        CommitRecordAttachmentRequest request = commitRequest(
                RecordAttachmentType.IMAGE,
                "flashback/users/1/records/10/image/11111111-1111-1111-1111-111111111111.jpg",
                "example.jpg",
                "image/jpeg",
                123456L);
        request.setWidth(1200);
        request.setHeight(800);

        var result = service.commitAttachment(1L, 10L, request);

        verify(recordAttachmentMapper).insert(org.mockito.ArgumentMatchers.argThat(attachment ->
                attachment.getRecordId().equals(10L)
                        && attachment.getUserId().equals(1L)
                        && attachment.getType() == RecordAttachmentType.IMAGE
                        && attachment.getStorageProvider() == com.flashback.domain.StorageProvider.QINIU
                        && "flashback-private".equals(attachment.getBucket())
                        && request.getKey().equals(attachment.getStorageKey())
                        && "image/jpeg".equals(attachment.getMimeType())
                        && attachment.getSortOrder().equals(3)
                        && attachment.getStatus() == com.flashback.domain.RecordAttachmentStatus.AVAILABLE));
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getRecordId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(com.flashback.domain.RecordAttachmentStatus.AVAILABLE);
        assertThat(result.getAccessUrl()).isNull();
    }

    @Test
    void shouldRejectCommitWhenKeyDoesNotBelongToRecord() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(0);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(0L);

        CommitRecordAttachmentRequest request = commitRequest(
                RecordAttachmentType.IMAGE,
                "flashback/users/2/records/10/image/wrong.jpg",
                "example.jpg",
                "image/jpeg",
                123456L);

        assertThatThrownBy(() -> service.commitAttachment(1L, 10L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("附件key不属于当前记录");
        verify(recordAttachmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectCommitWhenQiniuObjectMissing() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(0);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(0L);
        storageProvider.exception = new ObjectStorageException("missing", true);

        assertThatThrownBy(() -> service.commitAttachment(
                1L,
                10L,
                commitRequest(
                        RecordAttachmentType.IMAGE,
                        "flashback/users/1/records/10/image/missing.jpg",
                        "example.jpg",
                        "image/jpeg",
                        123456L)))
                .isInstanceOf(BizException.class)
                .hasMessage("上传文件验证失败");
        verify(recordAttachmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectCommitWhenQiniuSizeDiffers() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(10L, 1L, RecordAttachmentType.IMAGE))
                .thenReturn(0);
        when(recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(10L, 1L)).thenReturn(0L);
        ObjectStorageMetadata metadata = new ObjectStorageMetadata();
        metadata.setSizeBytes(123L);
        metadata.setMimeType("image/jpeg");
        storageProvider.metadata = metadata;

        assertThatThrownBy(() -> service.commitAttachment(
                1L,
                10L,
                commitRequest(
                        RecordAttachmentType.IMAGE,
                        "flashback/users/1/records/10/image/mismatch.jpg",
                        "example.jpg",
                        "image/jpeg",
                        123456L)))
                .isInstanceOf(BizException.class)
                .hasMessage("上传文件大小不一致");
        verify(recordAttachmentMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldCreatePrivateAccessUrlForOwnedAvailableAttachment() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.UNLOCKED));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/a.jpg"));

        var result = service.createAccessUrl(1L, 10L, 99L);

        assertThat(result.getAttachmentId()).isEqualTo(99L);
        assertThat(result.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 10, 10, 0));
        assertThat(result.getUrl()).startsWith(
                "https://media.example.com/flashback/users/1/records/10/image/a.jpg?e=1781748600&token=test-ak:");
    }

    @Test
    void shouldRejectAccessUrlWhenAttachmentNotOwned() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.UNLOCKED));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> service.createAccessUrl(1L, 10L, 99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("附件不存在");
    }

    @Test
    void shouldDeleteDraftAttachmentAfterQiniuDeleteSucceeds() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/a.jpg"));
        when(recordAttachmentMapper.markDeletedByIdAndRecordIdAndUserId(
                99L,
                10L,
                1L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0))).thenReturn(1);

        service.deleteAttachment(1L, 10L, 99L);

        assertThat(storageProvider.deletedBucket).isEqualTo("flashback-private");
        assertThat(storageProvider.deletedKey).isEqualTo("flashback/users/1/records/10/image/a.jpg");
        verify(recordAttachmentMapper).markDeletedByIdAndRecordIdAndUserId(
                99L,
                10L,
                1L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0));
        verify(recordMapper).clearCoverAttachmentIfMatches(
                10L,
                1L,
                99L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0));
    }

    @Test
    void shouldRejectDeletingLastEvidenceFromSavedRecord() {
        Record saved = record(RecordStatus.SAVED);
        saved.setContent("  ");
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(saved);
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/a.jpg"));
        when(recordAttachmentMapper.countAvailableByRecordIdAndUserId(10L, 1L)).thenReturn(1);

        assertThatThrownBy(() -> service.deleteAttachment(1L, 10L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessage("至少留下一句话、一张图片或一段声音");

        assertThat(storageProvider.deletedKey).isNull();
        verify(recordAttachmentMapper, never()).markDeletedByIdAndRecordIdAndUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldDeleteLocalMetadataWhenQiniuObjectAlreadyMissing() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/missing.jpg"));
        when(recordAttachmentMapper.markDeletedByIdAndRecordIdAndUserId(
                99L,
                10L,
                1L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0))).thenReturn(1);
        storageProvider.deleteException = new ObjectStorageException("missing", true);

        service.deleteAttachment(1L, 10L, 99L);

        verify(recordAttachmentMapper).markDeletedByIdAndRecordIdAndUserId(
                99L,
                10L,
                1L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0));
    }

    @Test
    void shouldRejectDeleteWhenRecordIsNotDraft() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.SEALED));

        assertThatThrownBy(() -> service.deleteAttachment(1L, 10L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessage("记录已封存，不能修改附件");
        verify(recordAttachmentMapper, never()).selectByIdAndRecordIdAndUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectDeleteWhenRecordIsUnlocked() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.UNLOCKED));

        assertThatThrownBy(() -> service.deleteAttachment(1L, 10L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessage("记录已封存，不能修改附件");
        verify(recordAttachmentMapper, never()).selectByIdAndRecordIdAndUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldKeepMetadataWhenQiniuDeleteFails() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/a.jpg"));
        storageProvider.deleteException = new ObjectStorageException("unavailable");

        assertThatThrownBy(() -> service.deleteAttachment(1L, 10L, 99L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getMessage()).isEqualTo("对象存储暂不可用");
                });
        verify(recordAttachmentMapper, never()).markDeletedByIdAndRecordIdAndUserId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private CreateAttachmentUploadTokenRequest request(
            RecordAttachmentType type,
            String fileName,
            String mimeType,
            long sizeBytes) {
        CreateAttachmentUploadTokenRequest request = new CreateAttachmentUploadTokenRequest();
        request.setType(type);
        request.setFileName(fileName);
        request.setMimeType(mimeType);
        request.setSizeBytes(sizeBytes);
        return request;
    }

    private CommitRecordAttachmentRequest commitRequest(
            RecordAttachmentType type,
            String key,
            String fileName,
            String mimeType,
            long sizeBytes) {
        CommitRecordAttachmentRequest request = new CommitRecordAttachmentRequest();
        request.setType(type);
        request.setKey(key);
        request.setFileName(fileName);
        request.setMimeType(mimeType);
        request.setSizeBytes(sizeBytes);
        return request;
    }

    private Record record(RecordStatus status) {
        Record record = new Record();
        record.setId(10L);
        record.setUserId(1L);
        record.setTitle("带附件的草稿");
        record.setContent("今天想留下图像和声音");
        record.setRecordType(RecordType.NODE_RECORD);
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.of(2026, 6, 18, 10, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 6, 18, 10, 0, 0));
        return record;
    }

    private RecordAttachment attachment(Long id, Long recordId, Long userId, String key) {
        RecordAttachment attachment = new RecordAttachment();
        attachment.setId(id);
        attachment.setRecordId(recordId);
        attachment.setUserId(userId);
        attachment.setType(RecordAttachmentType.IMAGE);
        attachment.setStatus(RecordAttachmentStatus.AVAILABLE);
        attachment.setStorageProvider(StorageProvider.QINIU);
        attachment.setBucket("flashback-private");
        attachment.setStorageKey(key);
        return attachment;
    }

    private static class StubObjectStorageProvider implements ObjectStorageProvider {

        private boolean configured = true;
        private ObjectStorageMetadata metadata;
        private ObjectStorageException exception;
        private ObjectStorageException deleteException;
        private String deletedBucket;
        private String deletedKey;

        @Override public StorageProvider getProvider() { return StorageProvider.QINIU; }
        @Override public boolean isConfigured() { return configured; }
        @Override public String getBucket() { return "flashback-private"; }
        @Override public String getKeyPrefix() { return "flashback"; }
        @Override public long getUploadAuthorizationTtlSeconds() { return 600; }
        @Override public long getDownloadUrlTtlSeconds() { return 600; }

        @Override
        public ObjectStorageUploadAuthorization createUploadAuthorization(
                String key, String mimeType, long sizeBytes, Instant expiresAt) {
            ObjectStorageUploadAuthorization authorization = new ObjectStorageUploadAuthorization();
            authorization.setMethod("POST_MULTIPART");
            authorization.setUploadUrl("https://upload.example.com");
            authorization.setFormData(Map.of("token", "test-upload-token", "key", key));
            return authorization;
        }

        @Override
        public ObjectStorageMetadata statObject(String bucket, String key) {
            if (exception != null) {
                throw exception;
            }
            return metadata;
        }

        @Override
        public void deleteObject(String bucket, String key) {
            deletedBucket = bucket;
            deletedKey = key;
            if (deleteException != null) {
                throw deleteException;
            }
        }

        @Override
        public String createPrivateAccessUrl(String bucket, String key, Instant expiresAt) {
            return "https://media.example.com/" + key + "?e=" + expiresAt.getEpochSecond() + "&token=test-ak:test";
        }
    }
}
