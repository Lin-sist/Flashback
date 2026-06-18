package com.flashback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppMediaProperties;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.service.RecordAttachmentService;
import com.flashback.vo.AttachmentUploadTokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * M4 record attachment service.
 */
@Service
public class RecordAttachmentServiceImpl implements RecordAttachmentService {

    private static final String QINIU_UPLOAD_URL = "https://upload.qiniup.com";
    private static final String QINIU_PROVIDER = "QINIU";
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
    private final AppStorageProperties appStorageProperties;
    private final AppMediaProperties appMediaProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    @Autowired
    public RecordAttachmentServiceImpl(
            RecordMapper recordMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            AppStorageProperties appStorageProperties,
            AppMediaProperties appMediaProperties,
            Clock clock) {
        this(
                recordMapper,
                recordAttachmentMapper,
                appStorageProperties,
                appMediaProperties,
                new ObjectMapper(),
                clock,
                UUID::randomUUID);
    }

    RecordAttachmentServiceImpl(
            RecordMapper recordMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            AppStorageProperties appStorageProperties,
            AppMediaProperties appMediaProperties,
            ObjectMapper objectMapper,
            Clock clock,
            Supplier<UUID> uuidSupplier) {
        this.recordMapper = recordMapper;
        this.recordAttachmentMapper = recordAttachmentMapper;
        this.appStorageProperties = appStorageProperties;
        this.appMediaProperties = appMediaProperties;
        this.objectMapper = objectMapper;
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
        AppStorageProperties.Qiniu qiniu = requireQiniuConfigured();

        String mimeType = normalizeMimeType(request.getMimeType());
        String extension = extensionFor(request.getType(), mimeType);
        validateSize(request.getSizeBytes());
        validateCount(recordId, userId, request.getType());
        validateTotalSize(recordId, userId, request.getSizeBytes());

        Instant expiresAtInstant = clock.instant().plusSeconds(qiniu.getUploadTokenTtlSeconds());
        long deadline = expiresAtInstant.getEpochSecond();
        String key = buildObjectKey(qiniu.getKeyPrefix(), userId, recordId, request.getType(), extension);
        String uploadToken = buildUploadToken(qiniu, key, request.getSizeBytes(), deadline);

        AttachmentUploadTokenVO vo = new AttachmentUploadTokenVO();
        vo.setProvider(QINIU_PROVIDER);
        vo.setBucket(qiniu.getBucket().trim());
        vo.setKey(key);
        vo.setUploadToken(uploadToken);
        vo.setUploadUrl(QINIU_UPLOAD_URL);
        vo.setExpiresAt(LocalDateTime.ofInstant(expiresAtInstant, clock.getZone()));
        vo.setMaxFileSizeBytes(appMediaProperties.getMaxFileSizeBytes());
        return vo;
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

    private AppStorageProperties.Qiniu requireQiniuConfigured() {
        AppStorageProperties.StorageProvider provider;
        try {
            provider = appStorageProperties.getProviderType();
        } catch (IllegalArgumentException ex) {
            throw serviceUnavailable("存储服务未配置");
        }
        if (provider != AppStorageProperties.StorageProvider.QINIU || !appStorageProperties.getQiniu().isConfigured()) {
            throw serviceUnavailable("存储服务未配置");
        }
        return appStorageProperties.getQiniu();
    }

    private String normalizeMimeType(String mimeType) {
        return mimeType == null ? "" : mimeType.trim().toLowerCase(Locale.ROOT);
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

    private String buildUploadToken(
            AppStorageProperties.Qiniu qiniu,
            String key,
            Long sizeBytes,
            long deadline) {
        try {
            Map<String, Object> putPolicy = new LinkedHashMap<>();
            putPolicy.put("scope", qiniu.getBucket().trim() + ":" + key);
            putPolicy.put("deadline", deadline);
            putPolicy.put("fsizeLimit", sizeBytes);

            String encodedPolicy = urlSafeBase64(objectMapper.writeValueAsString(putPolicy).getBytes(StandardCharsets.UTF_8));
            String encodedSign = hmacSha1(qiniu.getSecretKey().trim(), encodedPolicy);
            return qiniu.getAccessKey().trim() + ":" + encodedSign + ":" + encodedPolicy;
        } catch (Exception ex) {
            throw serviceUnavailable("上传凭证生成失败");
        }
    }

    private String hmacSha1(String secretKey, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return urlSafeBase64(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String urlSafeBase64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private BizException serviceUnavailable(String message) {
        return new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
