package com.flashback.storage;

import com.flashback.domain.StorageProvider;

import java.time.Instant;

public interface ObjectStorageProvider {

    StorageProvider getProvider();

    boolean isConfigured();

    String getBucket();

    String getKeyPrefix();

    long getUploadAuthorizationTtlSeconds();

    long getDownloadUrlTtlSeconds();

    ObjectStorageUploadAuthorization createUploadAuthorization(
            String key,
            String mimeType,
            long sizeBytes,
            Instant expiresAt);

    ObjectStorageMetadata statObject(String bucket, String key);

    void deleteObject(String bucket, String key);

    String createPrivateAccessUrl(String bucket, String key, Instant expiresAt);
}
