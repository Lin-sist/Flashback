package com.flashback.dto;

import com.flashback.domain.ReplyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建回信请求。
 */
public class CreateReplyRequest {

    @NotBlank(message = "content不能为空")
    @Size(max = 500, message = "content长度不能超过500")
    private String content;

    private ReplyType replyType;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ReplyType getReplyType() {
        return replyType;
    }

    public void setReplyType(ReplyType replyType) {
        this.replyType = replyType;
    }
}
