package com.flashback.config;

import com.flashback.domain.StorageProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * M4 object storage configuration.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class AppStorageProperties {

    @NotBlank
    private String provider = "qiniu";

    @Valid
    private Qiniu qiniu = new Qiniu();

    @Valid
    private S3 s3 = new S3();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public StorageProvider getProviderType() {
        return StorageProvider.fromConfigValue(provider);
    }

    public Qiniu getQiniu() {
        return qiniu;
    }

    public void setQiniu(Qiniu qiniu) {
        this.qiniu = qiniu;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }

    public static class Qiniu {

        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "";
        private String region = "";
        private String privateDomain = "";
        private String uploadUrl = "";

        @Positive
        private long uploadTokenTtlSeconds = 600;

        @Positive
        private long downloadUrlTtlSeconds = 600;

        @NotBlank
        private String keyPrefix = "flashback";

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getPrivateDomain() {
            return privateDomain;
        }

        public void setPrivateDomain(String privateDomain) {
            this.privateDomain = privateDomain;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public void setUploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
        }

        public long getUploadTokenTtlSeconds() {
            return uploadTokenTtlSeconds;
        }

        public void setUploadTokenTtlSeconds(long uploadTokenTtlSeconds) {
            this.uploadTokenTtlSeconds = uploadTokenTtlSeconds;
        }

        public long getDownloadUrlTtlSeconds() {
            return downloadUrlTtlSeconds;
        }

        public void setDownloadUrlTtlSeconds(long downloadUrlTtlSeconds) {
            this.downloadUrlTtlSeconds = downloadUrlTtlSeconds;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public boolean isConfigured() {
            return hasText(accessKey) && hasText(secretKey) && hasText(bucket) && hasText(privateDomain);
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }

    public static class S3 {

        private String endpoint = "";
        private String region = "";
        private String accessKey = "";
        private String secretKey = "";
        private String sessionToken = "";
        private String bucket = "";
        private boolean pathStyleAccess;

        @Positive
        private long uploadTokenTtlSeconds = 600;

        @Positive
        private long downloadUrlTtlSeconds = 600;

        @NotBlank
        private String keyPrefix = "flashback";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getSessionToken() { return sessionToken; }
        public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public boolean isPathStyleAccess() { return pathStyleAccess; }
        public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
        public long getUploadTokenTtlSeconds() { return uploadTokenTtlSeconds; }
        public void setUploadTokenTtlSeconds(long uploadTokenTtlSeconds) { this.uploadTokenTtlSeconds = uploadTokenTtlSeconds; }
        public long getDownloadUrlTtlSeconds() { return downloadUrlTtlSeconds; }
        public void setDownloadUrlTtlSeconds(long downloadUrlTtlSeconds) { this.downloadUrlTtlSeconds = downloadUrlTtlSeconds; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

        public boolean isConfigured() {
            return hasText(endpoint) && hasText(region) && hasText(accessKey) && hasText(secretKey) && hasText(bucket);
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
