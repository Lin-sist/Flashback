package com.flashback.dto;

import com.flashback.domain.RecordAttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * M4 attachment upload-token request.
 */
public class CreateAttachmentUploadTokenRequest {

    @NotNull(message = "type不能为空")
    private RecordAttachmentType type;

    @NotBlank(message = "fileName不能为空")
    @Size(max = 255, message = "fileName长度不能超过255")
    private String fileName;

    @NotBlank(message = "mimeType不能为空")
    @Size(max = 100, message = "mimeType长度不能超过100")
    private String mimeType;

    @NotNull(message = "sizeBytes不能为空")
    @Positive(message = "sizeBytes必须大于0")
    private Long sizeBytes;

    public RecordAttachmentType getType() {
        return type;
    }

    public void setType(RecordAttachmentType type) {
        this.type = type;
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
}
