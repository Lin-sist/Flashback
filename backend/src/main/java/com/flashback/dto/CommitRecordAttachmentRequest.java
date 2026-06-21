package com.flashback.dto;

import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.StorageProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * M4 attachment commit request after direct provider upload.
 */
public class CommitRecordAttachmentRequest {

    private StorageProvider provider;

    @NotNull(message = "type不能为空")
    private RecordAttachmentType type;

    @NotBlank(message = "key不能为空")
    @Size(max = 512, message = "key长度不能超过512")
    private String key;

    @NotBlank(message = "fileName不能为空")
    @Size(max = 255, message = "fileName长度不能超过255")
    private String fileName;

    @NotBlank(message = "mimeType不能为空")
    @Size(max = 100, message = "mimeType长度不能超过100")
    private String mimeType;

    @NotNull(message = "sizeBytes不能为空")
    @Positive(message = "sizeBytes必须大于0")
    private Long sizeBytes;

    @PositiveOrZero(message = "width不能小于0")
    private Integer width;

    @PositiveOrZero(message = "height不能小于0")
    private Integer height;

    @PositiveOrZero(message = "durationSeconds不能小于0")
    private Integer durationSeconds;

    public StorageProvider getProvider() { return provider; }

    public void setProvider(StorageProvider provider) { this.provider = provider; }

    public RecordAttachmentType getType() {
        return type;
    }

    public void setType(RecordAttachmentType type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
