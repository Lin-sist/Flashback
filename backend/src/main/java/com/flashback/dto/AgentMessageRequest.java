package com.flashback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户提交一轮对话消息。
 */
public class AgentMessageRequest {

    @NotBlank(message = "content不能为空")
    @Size(max = 1000, message = "content长度不能超过1000")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
