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

    // ---------- C4 agent-guardrails-hardening ----------

    /** 护栏加固配置。阈值全部走配置，且不引入任何凭证字段。 */
    private Guardrail guardrail = new Guardrail();

    public Guardrail getGuardrail() {
        return guardrail;
    }

    public void setGuardrail(Guardrail guardrail) {
        this.guardrail = guardrail == null ? new Guardrail() : guardrail;
    }

    // ---------- C3b agent-review-chat ----------

    /** 友人回看对话配置。 */
    private Review review = new Review();

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review == null ? new Review() : review;
    }

    /**
     * 回看对话配置（C3b）。
     *
     * 单列轮次上限而不复用 maxTurnsPerSession 的理由：回看是读后闲聊，
     * 节奏与「把此刻一点点说出来」的写作引导不同——引导需要走完情绪 → 困惑 →
     * 核心问题 → 期望四个阶段，回看没有要抵达的终点，聊到不想聊就该停。
     */
    public static class Review {

        /** 回看会话的轮次上限。默认比写作引导（8）更短。 */
        @Positive
        private int maxTurnsPerSession = 6;

        /** 注入 prompt 的回看记录内容最大字符数（正文 + 摘要 + 当时以为共用此上限）。 */
        @Positive
        private int recordExcerptChars = 400;

        public int getMaxTurnsPerSession() {
            return maxTurnsPerSession;
        }

        public void setMaxTurnsPerSession(int maxTurnsPerSession) {
            this.maxTurnsPerSession = maxTurnsPerSession;
        }

        public int getRecordExcerptChars() {
            return recordExcerptChars;
        }

        public void setRecordExcerptChars(int recordExcerptChars) {
            this.recordExcerptChars = recordExcerptChars;
        }
    }

    // ---------- C3 agent-memory-retrieval ----------

    /** 记忆检索与注入配置。同样不引入任何凭证字段。 */
    private Memory memory = new Memory();

    public Memory getMemory() {
        return memory;
    }

    public void setMemory(Memory memory) {
        this.memory = memory == null ? new Memory() : memory;
    }

    /**
     * 记忆检索与注入配置（C3）。
     *
     * 全部为预算与范围约束，不含任何相关性「魔法参数」——
     * 检索策略在代码中显式表达，便于测试与审查。
     */
    public static class Memory {

        /**
         * 记忆能力总开关。关闭时行为等价于 C4 现状，
         * 且后端会留下结构化痕迹说明记忆未生效，**不静默表现为检索无命中**。
         */
        private boolean enabled = true;

        /** 单轮最多注入的记忆片段条数。控制 token 预算与噪声。 */
        @Positive
        private int maxFragments = 3;

        /** 单条记忆片段注入时的最大字符数，超出截断。 */
        @Positive
        private int maxFragmentChars = 120;

        /** 检索的时间窗口（月）。超出窗口的记录不参与检索。 */
        @Positive
        private int lookbackMonths = 24;

        /**
         * 用于生成检索线索的最近用户消息条数。
         * 只取用户自己的表达，不含 Agent 的提问——否则会用 Agent 的措辞去检索用户的历史。
         */
        @Positive
        private int cueMessageWindow = 3;

        /** 单个检索关键词的最短长度，过滤掉无区分度的短词。 */
        @Positive
        private int minKeywordLength = 2;

        /** 单次检索最多使用的关键词数量。 */
        @Positive
        private int maxKeywords = 6;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxFragments() {
            return maxFragments;
        }

        public void setMaxFragments(int maxFragments) {
            this.maxFragments = maxFragments;
        }

        public int getMaxFragmentChars() {
            return maxFragmentChars;
        }

        public void setMaxFragmentChars(int maxFragmentChars) {
            this.maxFragmentChars = maxFragmentChars;
        }

        public int getLookbackMonths() {
            return lookbackMonths;
        }

        public void setLookbackMonths(int lookbackMonths) {
            this.lookbackMonths = lookbackMonths;
        }

        public int getCueMessageWindow() {
            return cueMessageWindow;
        }

        public void setCueMessageWindow(int cueMessageWindow) {
            this.cueMessageWindow = cueMessageWindow;
        }

        public int getMinKeywordLength() {
            return minKeywordLength;
        }

        public void setMinKeywordLength(int minKeywordLength) {
            this.minKeywordLength = minKeywordLength;
        }

        public int getMaxKeywords() {
            return maxKeywords;
        }

        public void setMaxKeywords(int maxKeywords) {
            this.maxKeywords = maxKeywords;
        }
    }

    /**
     * 护栏阈值与开关（C4）。
     *
     * 阈值初值为**保守推断而非实测标定**（design.md §3.3），
     * 须在闸门 3 用真实样本校准；校准前不得当作已验证阈值使用。
     */
    public static class Guardrail {

        /**
         * 忠实度判定总开关。关闭时后端会记录结构化日志说明判定未生效，
         * **不静默表现为判定通过**（backend-core delta 要求）。
         */
        private boolean faithfulnessEnabled = true;

        /** 后置内容检查（诊断 / 代决）总开关。 */
        private boolean contentCheckEnabled = true;

        /**
         * 忠实度比对的 n-gram 长度。
         * 取 4 的理由：中文 4-gram 足以避免「单字碰巧命中」的假覆盖，又不过分严格。
         */
        @Positive
        private int faithfulnessNgramSize = 4;

        /**
         * 候选文本被来源覆盖的最低比例。
         * 留出整理引入连接词与删减的空间；**不承担拦增写的主要职责**（见 maxUncoveredRun）。
         *
         * 实测校准（2026-07-28）：合法的「去口头语」整理实测覆盖率仅 0.500
         * （原话「嗯，那个，我最近就是那种睡不好，然后白天也没精神」→「我最近睡不好，白天也没精神」），
         * 因此规划阶段推断的 0.60 会误伤正常整理，下调为 0.35。
         */
        private double minCoverage = 0.35d;

        /**
         * 允许的最长连续未覆盖字符数——**增写的主判据**。
         * R1 的虚构句约 45 字远超此值；正常整理的接缝插入通常在 10 字以内。
         */
        @Positive
        private int maxUncoveredRun = 12;

        /**
         * 低于该长度的候选文本不做覆盖率判定，避免小样本抖动。
         * 注意：最长连续未覆盖片段判据仍然生效。
         */
        @Positive
        private int minCheckedLength = 12;

        /**
         * C3：触发时间归属要求的「仅记忆层覆盖」最短连续片段长度。
         *
         * 取 8 的理由：短于此长度的记忆命中多半是措辞巧合
         * （用户此刻与过去用了同一个常见短语），要求这种情况也带时间归属会大面积误伤。
         * 真正的复述——把过去写下的一句话说出来——通常远超此长度。
         *
         * **未经真实样本校准**（闸门 3 待办）；校准前不得当作已验证阈值。
         */
        @Positive
        private int minMemoryOnlyRunForAttribution = 8;

        public int getMinMemoryOnlyRunForAttribution() {
            return minMemoryOnlyRunForAttribution;
        }

        public void setMinMemoryOnlyRunForAttribution(int minMemoryOnlyRunForAttribution) {
            this.minMemoryOnlyRunForAttribution = minMemoryOnlyRunForAttribution;
        }

        public boolean isFaithfulnessEnabled() {
            return faithfulnessEnabled;
        }

        public void setFaithfulnessEnabled(boolean faithfulnessEnabled) {
            this.faithfulnessEnabled = faithfulnessEnabled;
        }

        public boolean isContentCheckEnabled() {
            return contentCheckEnabled;
        }

        public void setContentCheckEnabled(boolean contentCheckEnabled) {
            this.contentCheckEnabled = contentCheckEnabled;
        }

        public int getFaithfulnessNgramSize() {
            return faithfulnessNgramSize;
        }

        public void setFaithfulnessNgramSize(int faithfulnessNgramSize) {
            this.faithfulnessNgramSize = faithfulnessNgramSize;
        }

        public double getMinCoverage() {
            return minCoverage;
        }

        public void setMinCoverage(double minCoverage) {
            this.minCoverage = minCoverage;
        }

        public int getMaxUncoveredRun() {
            return maxUncoveredRun;
        }

        public void setMaxUncoveredRun(int maxUncoveredRun) {
            this.maxUncoveredRun = maxUncoveredRun;
        }

        public int getMinCheckedLength() {
            return minCheckedLength;
        }

        public void setMinCheckedLength(int minCheckedLength) {
            this.minCheckedLength = minCheckedLength;
        }
    }

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
