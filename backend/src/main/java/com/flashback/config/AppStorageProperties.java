package com.flashback.config;

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

    public enum StorageProvider {
        QINIU("qiniu");

        private final String configValue;

        StorageProvider(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static StorageProvider fromConfigValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return QINIU;
            }
            String normalized = value.trim();
            for (StorageProvider provider : values()) {
                if (provider.configValue.equalsIgnoreCase(normalized)
                        || provider.name().equalsIgnoreCase(normalized.replace('-', '_'))) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("Unsupported storage provider: " + value);
        }
    }

    public static class Qiniu {

        private String accessKey = "";
        private String secretKey = "";
        private String bucket = "";
        private String region = "";
        private String privateDomain = "";

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
}
