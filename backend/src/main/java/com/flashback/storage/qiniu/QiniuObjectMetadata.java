package com.flashback.storage.qiniu;

/**
 * Minimal Qiniu object stat metadata used by M4 attachment verification.
 */
public class QiniuObjectMetadata {

    private Long sizeBytes;
    private String mimeType;

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
