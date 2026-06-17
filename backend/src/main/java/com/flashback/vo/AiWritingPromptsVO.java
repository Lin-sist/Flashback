package com.flashback.vo;

import java.util.List;

/**
 * AI 写作提示结果。
 */
public class AiWritingPromptsVO {

    private List<String> prompts;
    private String source;
    private String status;
    private String message;

    public List<String> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<String> prompts) {
        this.prompts = prompts;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
