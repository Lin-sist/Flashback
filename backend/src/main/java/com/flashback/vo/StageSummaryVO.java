package com.flashback.vo;

import java.time.LocalDateTime;

/**
 * On-demand M3 stage summary response.
 */
public class StageSummaryVO {

    private String summary;
    private String source;
    private long recordCount;
    private long unlockedCount;
    private long lifeNodeCount;
    private LocalDateTime generatedAt;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(long recordCount) {
        this.recordCount = recordCount;
    }

    public long getUnlockedCount() {
        return unlockedCount;
    }

    public void setUnlockedCount(long unlockedCount) {
        this.unlockedCount = unlockedCount;
    }

    public long getLifeNodeCount() {
        return lifeNodeCount;
    }

    public void setLifeNodeCount(long lifeNodeCount) {
        this.lifeNodeCount = lifeNodeCount;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
