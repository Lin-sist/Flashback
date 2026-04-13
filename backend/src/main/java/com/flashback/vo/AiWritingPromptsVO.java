package com.flashback.vo;

import java.util.List;

/**
 * AI 写作提示结果。
 */
public class AiWritingPromptsVO {

    private List<String> prompts;
    private String source;

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
}
