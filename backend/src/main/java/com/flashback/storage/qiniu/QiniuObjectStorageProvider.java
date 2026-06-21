package com.flashback.storage.qiniu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.StorageProvider;
import com.flashback.storage.ObjectStorageException;
import com.flashback.storage.ObjectStorageMetadata;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageUploadAuthorization;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class QiniuObjectStorageProvider implements ObjectStorageProvider {

    private final AppStorageProperties properties;
    private final QiniuStorageClient client;
    private final ObjectMapper objectMapper;

    public QiniuObjectStorageProvider(
            AppStorageProperties properties,
            QiniuStorageClient client,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override public StorageProvider getProvider() { return StorageProvider.QINIU; }
    @Override public boolean isConfigured() { return properties.getQiniu().isConfigured(); }
    @Override public String getBucket() { return properties.getQiniu().getBucket().trim(); }
    @Override public String getKeyPrefix() { return properties.getQiniu().getKeyPrefix(); }
    @Override public long getUploadAuthorizationTtlSeconds() { return properties.getQiniu().getUploadTokenTtlSeconds(); }
    @Override public long getDownloadUrlTtlSeconds() { return properties.getQiniu().getDownloadUrlTtlSeconds(); }

    @Override
    public ObjectStorageUploadAuthorization createUploadAuthorization(
            String key, String mimeType, long sizeBytes, Instant expiresAt) {
        AppStorageProperties.Qiniu qiniu = properties.getQiniu();
        try {
            Map<String, Object> putPolicy = new LinkedHashMap<>();
            putPolicy.put("scope", getBucket() + ":" + key);
            putPolicy.put("deadline", expiresAt.getEpochSecond());
            putPolicy.put("fsizeLimit", sizeBytes);
            String encodedPolicy = urlSafeBase64(objectMapper.writeValueAsBytes(putPolicy));
            String encodedSign = hmacSha1(qiniu.getSecretKey().trim(), encodedPolicy);
            String uploadToken = qiniu.getAccessKey().trim() + ":" + encodedSign + ":" + encodedPolicy;

            ObjectStorageUploadAuthorization authorization = new ObjectStorageUploadAuthorization();
            authorization.setMethod("POST_MULTIPART");
            authorization.setUploadUrl(resolveUploadUrl(qiniu));
            authorization.setFileFieldName("file");
            authorization.setFormData(Map.of("token", uploadToken, "key", key));
            return authorization;
        } catch (Exception ex) {
            throw new ObjectStorageException("qiniu upload authorization failed", ex);
        }
    }

    @Override
    public ObjectStorageMetadata statObject(String bucket, String key) {
        try {
            QiniuObjectMetadata source = client.statObject(bucket, key);
            ObjectStorageMetadata target = new ObjectStorageMetadata();
            target.setSizeBytes(source == null ? null : source.getSizeBytes());
            target.setMimeType(source == null ? null : source.getMimeType());
            return target;
        } catch (QiniuStorageException ex) {
            throw new ObjectStorageException("qiniu stat failed", ex, ex.isNotFound());
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        try {
            client.deleteObject(bucket, key);
        } catch (QiniuStorageException ex) {
            throw new ObjectStorageException("qiniu delete failed", ex, ex.isNotFound());
        }
    }

    @Override
    public String createPrivateAccessUrl(String bucket, String key, Instant expiresAt) {
        AppStorageProperties.Qiniu qiniu = properties.getQiniu();
        String domain = qiniu.getPrivateDomain().trim();
        while (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        String urlWithDeadline = domain + "/" + key + "?e=" + expiresAt.getEpochSecond();
        try {
            return urlWithDeadline + "&token=" + qiniu.getAccessKey().trim() + ":"
                    + hmacSha1(qiniu.getSecretKey().trim(), urlWithDeadline);
        } catch (Exception ex) {
            throw new ObjectStorageException("qiniu access url failed", ex);
        }
    }

    private String resolveUploadUrl(AppStorageProperties.Qiniu qiniu) {
        if (qiniu.getUploadUrl() != null && !qiniu.getUploadUrl().trim().isEmpty()) {
            return qiniu.getUploadUrl().trim();
        }
        String region = qiniu.getRegion() == null ? "" : qiniu.getRegion().trim().toLowerCase(Locale.ROOT);
        return switch (region) {
            case "z1", "cn-north-1" -> "https://upload-z1.qiniup.com";
            case "z2", "cn-south-1" -> "https://upload-z2.qiniup.com";
            case "na0", "us-north-1" -> "https://upload-na0.qiniup.com";
            case "as0", "ap-southeast-1" -> "https://upload-as0.qiniup.com";
            case "ap-ne-1" -> "https://upload-ap-ne-1.qiniup.com";
            default -> "https://upload.qiniup.com";
        };
    }

    private String hmacSha1(String secretKey, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return urlSafeBase64(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String urlSafeBase64(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
