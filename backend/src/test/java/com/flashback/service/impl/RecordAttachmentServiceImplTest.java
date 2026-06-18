package com.flashback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppMediaProperties;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
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
        service = new RecordAttachmentServiceImpl(
                recordMapper,
                recordAttachmentMapper,
                storageProperties,
                mediaProperties,
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
}
