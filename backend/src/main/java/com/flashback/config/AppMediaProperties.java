package com.flashback.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * M4 record media limits.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.media")
public class AppMediaProperties {

    @Positive
    private int maxImageCountPerRecord = 9;

    @Positive
    private int maxVoiceCountPerRecord = 9;

    @Positive
    private long maxFileSizeBytes = 41943040;

    @Positive
    private long maxTotalSizeBytesPerRecord = 314572800;

    public int getMaxImageCountPerRecord() {
        return maxImageCountPerRecord;
    }

    public void setMaxImageCountPerRecord(int maxImageCountPerRecord) {
        this.maxImageCountPerRecord = maxImageCountPerRecord;
    }

    public int getMaxVoiceCountPerRecord() {
        return maxVoiceCountPerRecord;
    }

    public void setMaxVoiceCountPerRecord(int maxVoiceCountPerRecord) {
        this.maxVoiceCountPerRecord = maxVoiceCountPerRecord;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public long getMaxTotalSizeBytesPerRecord() {
        return maxTotalSizeBytesPerRecord;
    }

    public void setMaxTotalSizeBytesPerRecord(long maxTotalSizeBytesPerRecord) {
        this.maxTotalSizeBytesPerRecord = maxTotalSizeBytesPerRecord;
    }
}
