package com.flashback.domain;

/**
 * Record media storage provider.
 */
public enum StorageProvider {
    QINIU,
    S3_COMPATIBLE;

    public static StorageProvider fromConfigValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return QINIU;
        }
        return switch (value.trim().toLowerCase().replace('_', '-')) {
            case "qiniu" -> QINIU;
            case "s3", "s3-compatible", "aws-s3", "aliyun-oss", "tencent-cos", "minio" -> S3_COMPATIBLE;
            default -> throw new IllegalArgumentException("Unsupported storage provider: " + value);
        };
    }
}
