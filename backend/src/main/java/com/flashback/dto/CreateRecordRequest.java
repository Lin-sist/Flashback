package com.flashback.dto;

import com.flashback.domain.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新建记录请求。
 */
public class CreateRecordRequest {

    @Size(max = 100, message = "title长度不能超过100")
    private String title;

    @NotBlank(message = "content不能为空")
    private String content;

    @NotNull(message = "recordType不能为空")
    private RecordType recordType;

    @Size(max = 255, message = "coreQuestion长度不能超过255")
    private String coreQuestion;

    @Size(max = 2000, message = "aiSummary长度不能超过2000")
    private String aiSummary;

    @Size(max = 10, message = "aiPromptResults数量不能超过10")
    private List<@Size(max = 255, message = "aiPromptResults单项长度不能超过255") String> aiPromptResults;

    private LocalDateTime unlockAt;

    @Size(max = 20, message = "tagIds数量不能超过20")
    private List<Long> tagIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public List<String> getAiPromptResults() {
        return aiPromptResults;
    }

    public void setAiPromptResults(List<String> aiPromptResults) {
        this.aiPromptResults = aiPromptResults;
    }

    public LocalDateTime getUnlockAt() {
        return unlockAt;
    }

    public void setUnlockAt(LocalDateTime unlockAt) {
        this.unlockAt = unlockAt;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}
