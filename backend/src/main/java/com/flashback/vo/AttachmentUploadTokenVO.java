package com.flashback.vo;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M4 provider-neutral upload authorization response.
 */
public class AttachmentUploadTokenVO {

    private String provider;
    private String bucket;
    private String key;
    private String uploadMethod;
    private String uploadUrl;
    private String fileFieldName;
    private Map<String, String> uploadHeaders = new LinkedHashMap<>();
    private Map<String, String> uploadFormData = new LinkedHashMap<>();
    private LocalDateTime expiresAt;
    private Long maxFileSizeBytes;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUploadMethod() { return uploadMethod; }
    public void setUploadMethod(String uploadMethod) { this.uploadMethod = uploadMethod; }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getFileFieldName() { return fileFieldName; }
    public void setFileFieldName(String fileFieldName) { this.fileFieldName = fileFieldName; }
    public Map<String, String> getUploadHeaders() { return uploadHeaders; }
    public void setUploadHeaders(Map<String, String> uploadHeaders) { this.uploadHeaders = uploadHeaders; }
    public Map<String, String> getUploadFormData() { return uploadFormData; }
    public void setUploadFormData(Map<String, String> uploadFormData) { this.uploadFormData = uploadFormData; }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(Long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
}
