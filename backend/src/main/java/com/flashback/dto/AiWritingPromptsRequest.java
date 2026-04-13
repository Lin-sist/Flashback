package com.flashback.dto;

import jakarta.validation.constraints.Size;

/**
 * AI 写作提示请求。
 */
public class AiWritingPromptsRequest {

    @Size(max = 5000, message = "content长度不能超过5000")
    private String content;

    @Size(max = 30, message = "recordType长度不能超过30")
    private String recordType;

    @Size(max = 255, message = "coreQuestion长度不能超过255")
    private String coreQuestion;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }
}
