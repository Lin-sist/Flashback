package com.flashback.vo;

import java.time.LocalDateTime;

/**
 * Short-lived private media access URL.
 */
public class AttachmentAccessUrlVO {

    private Long attachmentId;
    private String url;
    private LocalDateTime expiresAt;

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
