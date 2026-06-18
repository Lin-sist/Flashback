package com.flashback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.storage.qiniu.QiniuObjectMetadata;
import com.flashback.storage.qiniu.QiniuStorageClient;
import com.flashback.storage.qiniu.QiniuStorageException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
    private StubQiniuStorageClient qiniuStorageClient;
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
        qiniuStorageClient = new StubQiniuStorageClient();
        service = new RecordAttachmentServiceImpl(
                recordMapper,
                recordAttachmentMapper,
                storageProperties,
                mediaProperties,
                qiniuStorageClient,
                new ObjectMapper(),
                clock,
                () -> UUID.fromString("11111111-1111-1111-1111-111111111111"));
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
        assertThat(result.getUploadUrl()).isEqualTo("https://upload.qiniup.com");
        assertThat(result.getExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 10, 10, 0));
        assertThat(result.getMaxFileSizeBytes()).isEqualTo(41943040L);
        assertThat(result.getUploadToken()).startsWith("test-ak:");
        assertThat(result.getUploadToken().split(":")).hasSize(3);
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
        storageProperties.getQiniu().setSecretKey("");
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
        QiniuObjectMetadata metadata = new QiniuObjectMetadata();
        metadata.setSizeBytes(123456L);
        metadata.setMimeType("image/jpeg");
        qiniuStorageClient.metadata = metadata;

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
        qiniuStorageClient.exception = new QiniuStorageException("missing", true);

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
        QiniuObjectMetadata metadata = new QiniuObjectMetadata();
        metadata.setSizeBytes(123L);
        metadata.setMimeType("image/jpeg");
        qiniuStorageClient.metadata = metadata;

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

        assertThat(qiniuStorageClient.deletedBucket).isEqualTo("flashback-private");
        assertThat(qiniuStorageClient.deletedKey).isEqualTo("flashback/users/1/records/10/image/a.jpg");
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
    void shouldDeleteLocalMetadataWhenQiniuObjectAlreadyMissing() {
        when(recordMapper.selectByIdAndUserId(10L, 1L)).thenReturn(record(RecordStatus.DRAFT));
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(99L, 10L, 1L))
                .thenReturn(attachment(99L, 10L, 1L, "flashback/users/1/records/10/image/missing.jpg"));
        when(recordAttachmentMapper.markDeletedByIdAndRecordIdAndUserId(
                99L,
                10L,
                1L,
                LocalDateTime.of(2026, 6, 18, 10, 0, 0))).thenReturn(1);
        qiniuStorageClient.deleteException = new QiniuStorageException("missing", true);

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
        qiniuStorageClient.deleteException = new QiniuStorageException("unavailable");

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
        attachment.setBucket("flashback-private");
        attachment.setStorageKey(key);
        return attachment;
    }

    private static class StubQiniuStorageClient implements QiniuStorageClient {

        private QiniuObjectMetadata metadata;
        private QiniuStorageException exception;
        private QiniuStorageException deleteException;
        private String deletedBucket;
        private String deletedKey;

        @Override
        public QiniuObjectMetadata statObject(String bucket, String key) {
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
    }
}
