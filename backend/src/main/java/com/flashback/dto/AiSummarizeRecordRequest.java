package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 内容整理请求。
 */
public class AiSummarizeRecordRequest {

    @NotBlank(message = "content不能为空")
    @Size(max = 5000, message = "content长度不能超过5000")
    private String content;

    @Size(max = 255, message = "coreQuestion长度不能超过255")
    private String coreQuestion;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }
}
