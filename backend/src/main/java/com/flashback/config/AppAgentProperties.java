package com.flashback.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Agent Runtime 配置项（C1）。
 *
 * 注意：Agent 复用 app.ai 的 provider / secret 配置，本类只承载 Runtime 侧参数，
 * 不引入任何新的凭证字段。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.agent")
public class AppAgentProperties {

    /** 单个会话允许的最大轮次，达到后强制收束。 */
    @Positive
    private int maxTurnsPerSession = 8;

    /** 单条 Agent 回复的最大字符数，超出由后端裁剪（C1 唯一代码级硬护栏）。 */
    @Positive
    private int maxReplyChars = 120;

    /** 单条用户输入的最大字符数。 */
    @Positive
    private int maxUserInputChars = 1000;

    /** 组装上下文时携带的历史消息条数上限（滑动窗口）。 */
    @Positive
    private int contextMessageWindow = 12;

    /** 注入 prompt 的草稿正文引用最大字符数，避免上下文膨胀。 */
    @Positive
    private int draftExcerptChars = 300;

    public int getMaxTurnsPerSession() {
        return maxTurnsPerSession;
    }

    public void setMaxTurnsPerSession(int maxTurnsPerSession) {
        this.maxTurnsPerSession = maxTurnsPerSession;
    }

    public int getMaxReplyChars() {
        return maxReplyChars;
    }

    public void setMaxReplyChars(int maxReplyChars) {
        this.maxReplyChars = maxReplyChars;
    }

    public int getMaxUserInputChars() {
        return maxUserInputChars;
    }

    public void setMaxUserInputChars(int maxUserInputChars) {
        this.maxUserInputChars = maxUserInputChars;
    }

    public int getContextMessageWindow() {
        return contextMessageWindow;
    }

    public void setContextMessageWindow(int contextMessageWindow) {
        this.contextMessageWindow = contextMessageWindow;
    }

    public int getDraftExcerptChars() {
        return draftExcerptChars;
    }

    public void setDraftExcerptChars(int draftExcerptChars) {
        this.draftExcerptChars = draftExcerptChars;
    }
}
