package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppMediaProperties;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.StorageProvider;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.service.RecordAttachmentService;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageMetadata;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import com.flashback.storage.ObjectStorageUploadAuthorization;
import com.flashback.vo.AttachmentAccessUrlVO;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.RecordAttachmentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * M4 record attachment service.
 */
@Service
public class RecordAttachmentServiceImpl implements RecordAttachmentService {

    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic");
    private static final Set<String> VOICE_MIME_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/wav",
            "audio/x-wav",
            "audio/amr",
            "audio/m4a");

    private final RecordMapper recordMapper;
    private final RecordAttachmentMapper recordAttachmentMapper;
    private final AppMediaProperties appMediaProperties;
    private final ObjectStorageRegistry objectStorageRegistry;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    @Autowired
    public RecordAttachmentServiceImpl(
            RecordMapper recordMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            AppMediaProperties appMediaProperties,
            ObjectStorageRegistry objectStorageRegistry,
            Clock clock) {
        this(
                recordMapper,
                recordAttachmentMapper,
                appMediaProperties,
                objectStorageRegistry,
                clock,
                UUID::randomUUID);
    }

    RecordAttachmentServiceImpl(
            RecordMapper recordMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            AppMediaProperties appMediaProperties,
            ObjectStorageRegistry objectStorageRegistry,
            Clock clock,
            Supplier<UUID> uuidSupplier) {
        this.recordMapper = recordMapper;
        this.recordAttachmentMapper = recordAttachmentMapper;
        this.appMediaProperties = appMediaProperties;
        this.objectStorageRegistry = objectStorageRegistry;
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }

    @Override
    public AttachmentUploadTokenVO createUploadToken(
            Long userId,
            Long recordId,
            CreateAttachmentUploadTokenRequest request) {
        Record record = requireOwnedRecord(recordId, userId);
        ensureDraft(record);
        ObjectStorageProvider storage = requireActiveStorage();

        String mimeType = normalizeMimeType(request.getMimeType());
        String extension = extensionFor(request.getType(), mimeType);
        validateSize(request.getSizeBytes());
        validateCount(recordId, userId, request.getType());
        validateTotalSize(recordId, userId, request.getSizeBytes());

        Instant expiresAtInstant = clock.instant().plusSeconds(storage.getUploadAuthorizationTtlSeconds());
        String key = buildObjectKey(storage.getKeyPrefix(), userId, recordId, request.getType(), extension);
        ObjectStorageUploadAuthorization authorization;
        try {
            authorization = storage.createUploadAuthorization(
                    key,
                    mimeType,
                    request.getSizeBytes(),
                    expiresAtInstant);
        } catch (ObjectStorageException ex) {
            throw serviceUnavailable("上传凭证生成失败");
        }

        AttachmentUploadTokenVO vo = new AttachmentUploadTokenVO();
        vo.setProvider(storage.getProvider().name());
        vo.setBucket(storage.getBucket());
        vo.setKey(key);
        vo.setUploadMethod(authorization.getMethod());
        vo.setUploadUrl(authorization.getUploadUrl());
        vo.setFileFieldName(authorization.getFileFieldName());
        vo.setUploadHeaders(authorization.getHeaders());
        vo.setUploadFormData(authorization.getFormData());
        vo.setExpiresAt(LocalDateTime.ofInstant(expiresAtInstant, clock.getZone()));
        vo.setMaxFileSizeBytes(appMediaProperties.getMaxFileSizeBytes());
        return vo;
    }

    @Override
    public RecordAttachmentVO commitAttachment(
            Long userId,
            Long recordId,
            CommitRecordAttachmentRequest request) {
        Record record = requireOwnedRecord(recordId, userId);
        ensureDraft(record);
        ObjectStorageProvider storage = request.getProvider() == null
                ? requireActiveStorage()
                : requireStorage(request.getProvider());

        String mimeType = normalizeMimeType(request.getMimeType());
        extensionFor(request.getType(), mimeType);
        validateSize(request.getSizeBytes());
        validateCount(recordId, userId, request.getType());
        validateTotalSize(recordId, userId, request.getSizeBytes());
        String key = normalizeStorageKey(request.getKey());
        validateKeyNamespace(storage.getKeyPrefix(), key, userId, recordId, request.getType());
        verifyUploadedObject(storage, storage.getBucket(), key, request.getSizeBytes(), mimeType);

        LocalDateTime now = LocalDateTime.now(clock);
        RecordAttachment attachment = new RecordAttachment();
        attachment.setRecordId(recordId);
        attachment.setUserId(userId);
        attachment.setType(request.getType());
        attachment.setStorageProvider(storage.getProvider());
        attachment.setBucket(storage.getBucket());
        attachment.setStorageKey(key);
        attachment.setFileName(normalizeRequired(request.getFileName(), "fileName不能为空"));
        attachment.setMimeType(mimeType);
        attachment.setSizeBytes(request.getSizeBytes());
        attachment.setWidth(request.getWidth());
        attachment.setHeight(request.getHeight());
        attachment.setDurationSeconds(request.getDurationSeconds());
        attachment.setSortOrder(recordAttachmentMapper.countAvailableByRecordIdAndUserId(recordId, userId));
        attachment.setStatus(RecordAttachmentStatus.AVAILABLE);
        attachment.setCreatedAt(now);
        attachment.setUpdatedAt(now);
        recordAttachmentMapper.insert(attachment);
        return toVO(attachment);
    }

    @Override
    public AttachmentAccessUrlVO createAccessUrl(Long userId, Long recordId, Long attachmentId) {
        requireOwnedRecord(recordId, userId);
        RecordAttachment attachment = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(
                attachmentId,
                recordId,
                userId);
        if (attachment == null || attachment.getStatus() != RecordAttachmentStatus.AVAILABLE) {
            throw new NotFoundException("附件不存在");
        }
        ObjectStorageProvider storage = requireStorage(attachment.getStorageProvider());

        Instant expiresAtInstant = clock.instant().plusSeconds(storage.getDownloadUrlTtlSeconds());
        String accessUrl;
        try {
            accessUrl = storage.createPrivateAccessUrl(
                    attachment.getBucket(),
                    attachment.getStorageKey(),
                    expiresAtInstant);
        } catch (ObjectStorageException ex) {
            throw serviceUnavailable("媒体访问地址生成失败");
        }

        AttachmentAccessUrlVO vo = new AttachmentAccessUrlVO();
        vo.setAttachmentId(attachmentId);
        vo.setUrl(accessUrl);
        vo.setExpiresAt(LocalDateTime.ofInstant(expiresAtInstant, clock.getZone()));
        return vo;
    }

    @Override
    public void deleteAttachment(Long userId, Long recordId, Long attachmentId) {
        Record record = requireOwnedRecord(recordId, userId);
        ensureDraft(record);

        RecordAttachment attachment = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(
                attachmentId,
                recordId,
                userId);
        if (attachment == null || attachment.getStatus() != RecordAttachmentStatus.AVAILABLE) {
            throw new NotFoundException("附件不存在");
        }
        ObjectStorageProvider storage = requireStorage(attachment.getStorageProvider());

        try {
            storage.deleteObject(attachment.getBucket(), attachment.getStorageKey());
        } catch (ObjectStorageException ex) {
            if (!ex.isNotFound()) {
                throw serviceUnavailable("对象存储暂不可用");
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int updated = recordAttachmentMapper.markDeletedByIdAndRecordIdAndUserId(
                attachmentId,
                recordId,
                userId,
                now);
        if (updated == 0) {
            throw new NotFoundException("附件不存在");
        }
        recordMapper.clearCoverAttachmentIfMatches(
                recordId,
                userId,
                attachmentId,
                now);
    }

    private Record requireOwnedRecord(Long recordId, Long userId) {
        Record record = recordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new NotFoundException("记录不存在");
        }
        return record;
    }

    private void ensureDraft(Record record) {
        if (record.getStatus() != RecordStatus.DRAFT) {
            throw badRequest("记录已封存，不能修改附件");
        }
    }

    private ObjectStorageProvider requireActiveStorage() {
        try {
            return objectStorageRegistry.getActiveProvider();
        } catch (ObjectStorageException ex) {
            throw serviceUnavailable("存储服务未配置");
        }
    }

    private ObjectStorageProvider requireStorage(StorageProvider provider) {
        if (provider == null) {
            throw serviceUnavailable("存储服务未配置");
        }
        try {
            return objectStorageRegistry.getRequired(provider);
        } catch (ObjectStorageException ex) {
            throw serviceUnavailable("存储服务未配置");
        }
    }

    private String normalizeMimeType(String mimeType) {
        return mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStorageKey(String key) {
        String normalized = key == null ? "" : key.trim();
        if (normalized.isEmpty()) {
            throw badRequest("key不能为空");
        }
        if (normalized.contains("..")
                || normalized.startsWith("/")
                || normalized.contains("?")
                || normalized.contains("#")) {
            throw badRequest("附件key不合法");
        }
        return normalized;
    }

    private String extensionFor(RecordAttachmentType type, String mimeType) {
        if (type == RecordAttachmentType.IMAGE && IMAGE_MIME_TYPES.contains(mimeType)) {
            return switch (mimeType) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/heic" -> "heic";
                default -> throw badRequest("图片类型不支持");
            };
        }
        if (type == RecordAttachmentType.VOICE && VOICE_MIME_TYPES.contains(mimeType)) {
            return switch (mimeType) {
                case "audio/mpeg" -> "mp3";
                case "audio/mp4", "audio/m4a" -> "m4a";
                case "audio/aac" -> "aac";
                case "audio/wav", "audio/x-wav" -> "wav";
                case "audio/amr" -> "amr";
                default -> throw badRequest("语音类型不支持");
            };
        }
        throw badRequest(type == RecordAttachmentType.IMAGE ? "图片类型不支持" : "语音类型不支持");
    }

    private void validateSize(Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) {
            throw badRequest("文件大小必须大于0");
        }
        if (sizeBytes > appMediaProperties.getMaxFileSizeBytes()) {
            throw badRequest("单个附件不能超过40MB");
        }
    }

    private void validateCount(Long recordId, Long userId, RecordAttachmentType type) {
        int existingCount = recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(recordId, userId, type);
        int maxCount = type == RecordAttachmentType.IMAGE
                ? appMediaProperties.getMaxImageCountPerRecord()
                : appMediaProperties.getMaxVoiceCountPerRecord();
        if (existingCount >= maxCount) {
            throw badRequest(type == RecordAttachmentType.IMAGE ? "图片数量不能超过9张" : "语音数量不能超过9条");
        }
    }

    private void validateTotalSize(Long recordId, Long userId, Long requestedSizeBytes) {
        long currentSize = recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(recordId, userId);
        if (currentSize + requestedSizeBytes > appMediaProperties.getMaxTotalSizeBytesPerRecord()) {
            throw badRequest("单条记录附件总大小不能超过300MB");
        }
    }

    private String buildObjectKey(
            String keyPrefix,
            Long userId,
            Long recordId,
            RecordAttachmentType type,
            String extension) {
        String prefix = normalizeKeyPrefix(keyPrefix);
        String typePath = type == RecordAttachmentType.IMAGE ? "image" : "voice";
        return "%s/users/%d/records/%d/%s/%s.%s".formatted(
                prefix,
                userId,
                recordId,
                typePath,
                uuidSupplier.get(),
                extension);
    }

    private String normalizeKeyPrefix(String keyPrefix) {
        String normalized = keyPrefix == null ? "flashback" : keyPrefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "flashback" : normalized;
    }

    private void validateKeyNamespace(
            String keyPrefix,
            String key,
            Long userId,
            Long recordId,
            RecordAttachmentType type) {
        String typePath = type == RecordAttachmentType.IMAGE ? "image" : "voice";
        String expectedPrefix = "%s/users/%d/records/%d/%s/".formatted(
                normalizeKeyPrefix(keyPrefix),
                userId,
                recordId,
                typePath);
        if (!key.startsWith(expectedPrefix)) {
            throw badRequest("附件key不属于当前记录");
        }
    }

    private void verifyUploadedObject(
            ObjectStorageProvider storage,
            String bucket,
            String key,
            Long expectedSizeBytes,
            String expectedMimeType) {
        ObjectStorageMetadata metadata;
        try {
            metadata = storage.statObject(bucket, key);
        } catch (ObjectStorageException ex) {
            if (ex.isNotFound()) {
                throw badRequest("上传文件验证失败");
            }
            throw serviceUnavailable("对象存储暂不可用");
        }
        if (metadata == null || metadata.getSizeBytes() == null) {
            throw badRequest("上传文件验证失败");
        }
        if (!metadata.getSizeBytes().equals(expectedSizeBytes)) {
            throw badRequest("上传文件大小不一致");
        }
        String actualMimeType = normalizeMimeType(metadata.getMimeType());
        if (!actualMimeType.isEmpty() && !actualMimeType.equals(expectedMimeType)) {
            throw badRequest("上传文件类型不一致");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw badRequest(message);
        }
        return normalized;
    }

    private RecordAttachmentVO toVO(RecordAttachment attachment) {
        RecordAttachmentVO vo = new RecordAttachmentVO();
        vo.setId(attachment.getId());
        vo.setRecordId(attachment.getRecordId());
        vo.setType(attachment.getType());
        vo.setStatus(attachment.getStatus());
        vo.setFileName(attachment.getFileName());
        vo.setMimeType(attachment.getMimeType());
        vo.setSizeBytes(attachment.getSizeBytes());
        vo.setWidth(attachment.getWidth());
        vo.setHeight(attachment.getHeight());
        vo.setDurationSeconds(attachment.getDurationSeconds());
        vo.setSortOrder(attachment.getSortOrder());
        vo.setCreatedAt(attachment.getCreatedAt());
        vo.setAccessUrl(null);
        return vo;
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private BizException serviceUnavailable(String message) {
        return new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

}
