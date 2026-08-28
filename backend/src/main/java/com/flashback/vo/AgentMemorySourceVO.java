package com.flashback.vo;

import java.time.LocalDateTime;

/** P4.2：用户可见的实际来源。不包含片段、摘要、分数或关键词。 */
public class AgentMemorySourceVO {

    private Long recordId;
    private String sourceKind;
    private String displayTitle;
    private LocalDateTime occurredAt;
    private String contextNote;
    private boolean available;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(String sourceKind) {
        this.sourceKind = sourceKind;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getContextNote() {
        return contextNote;
    }

    public void setContextNote(String contextNote) {
        this.contextNote = contextNote;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
