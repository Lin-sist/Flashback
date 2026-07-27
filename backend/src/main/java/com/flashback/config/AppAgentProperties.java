package com.flashback.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent Runtime 配置项（C1 + C2）。
 *
 * 注意：Agent 复用 app.ai 的 provider / secret 配置，本类只承载 Runtime 侧参数，
 * 不引入任何新的凭证字段。C2 新增的 strictModeBaseUrl 是**地址**而非凭证。
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

    // ---------- C2 agent-tool-calling ----------

    /**
     * 工具调用总开关。关闭时 Agent 退回 C1 纯对话行为，
     * **不是**降级到任何自研提议协议（design.md 决策 1：无降级）。
     */
    private boolean toolCallingEnabled = true;

    /**
     * 是否启用 provider 的 strict mode（服务端校验工具 JSON Schema）。
     * strict mode 需要配合独立的 base URL，见 strictModeBaseUrl。
     */
    private boolean strictModeEnabled = false;

    /**
     * strict mode 专用 base URL（provider 的 beta 端点）。
     * 默认空；strictModeEnabled=true 而本项为空时视为配置错误，不静默降级。
     */
    private String strictModeBaseUrl = "";

    /**
     * 已确认支持 function calling 的 model 白名单。
     * 当前 app.ai.model 不在其中时不下发 tools（proposal F29：
     * 不得假设任意 OPENAI_COMPATIBLE provider / 任意 model 都支持 FC）。
     */
    private List<String> functionCallingModels = new ArrayList<>(List.of("deepseek-v4-pro", "deepseek-v4-flash"));

    /** 单次 append_record_content 追加素材的最大字符数（strict 无法表达，故代码层校验）。 */
    @Positive
    private int maxToolContentChars = 300;

    /** 单次 add_record_tags 可追加的标签数量上限（strict 无法表达，故代码层校验）。 */
    @Positive
    private int maxToolTagIds = 5;

    /** 组装上下文时回注的最近工具执行结果条数上限。 */
    @Positive
    private int toolOutcomeWindow = 3;

    public boolean isToolCallingEnabled() {
        return toolCallingEnabled;
    }

    public void setToolCallingEnabled(boolean toolCallingEnabled) {
        this.toolCallingEnabled = toolCallingEnabled;
    }

    public boolean isStrictModeEnabled() {
        return strictModeEnabled;
    }

    public void setStrictModeEnabled(boolean strictModeEnabled) {
        this.strictModeEnabled = strictModeEnabled;
    }

    public String getStrictModeBaseUrl() {
        return strictModeBaseUrl;
    }

    public void setStrictModeBaseUrl(String strictModeBaseUrl) {
        this.strictModeBaseUrl = strictModeBaseUrl;
    }

    public List<String> getFunctionCallingModels() {
        return functionCallingModels;
    }

    public void setFunctionCallingModels(List<String> functionCallingModels) {
        this.functionCallingModels = functionCallingModels == null ? new ArrayList<>() : functionCallingModels;
    }

    public int getMaxToolContentChars() {
        return maxToolContentChars;
    }

    public void setMaxToolContentChars(int maxToolContentChars) {
        this.maxToolContentChars = maxToolContentChars;
    }

    public int getMaxToolTagIds() {
        return maxToolTagIds;
    }

    public void setMaxToolTagIds(int maxToolTagIds) {
        this.maxToolTagIds = maxToolTagIds;
    }

    public int getToolOutcomeWindow() {
        return toolOutcomeWindow;
    }

    public void setToolOutcomeWindow(int toolOutcomeWindow) {
        this.toolOutcomeWindow = toolOutcomeWindow;
    }

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
