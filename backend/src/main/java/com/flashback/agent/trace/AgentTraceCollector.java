package com.flashback.agent.trace;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.AgentStageDecision;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.agent.reflection.AgentProviderPhase;
import com.flashback.agent.reflection.AgentReflectionTerminal;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentStage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 一轮对话的决策轨迹收集器（C5）。
 *
 * 存在理由（design.md 决策 2）：轨迹的采集点有七八处，散落在编排的不同分支里。
 * 若每处各自 insert，provider 失败、护栏降级、fail-closed 丢弃这些**早退路径**
 * 最容易被跳过——而那正是最需要痕迹的时刻。本类把一轮内的全部步骤先收在内存里，
 * 由 {@link AgentTraceSink} 在唯一出口落库。
 *
 * <h3>隐私（design.md §2.3，本类最重要的约束）</h3>
 * 本类的收集方法**只接受基础类型与既有枚举**，不接受任意文本内容参数——
 * 想把日记原文、对话原文、记忆片段或护栏候选文本传进轨迹，在类型层就做不到。
 * 少数 {@code String} 形参（model、causeType、rejectReason）承载的都是
 * 配置值、异常类名或结构化常量短标识，不含用户表达。
 *
 * 本类**不是线程安全的**，也不需要——一个实例只服务一轮对话，生命周期在单次请求内。
 */
public final class AgentTraceCollector {

    private final String traceId;
    private final Long sessionId;
    private final Long userId;
    private final Long recordId;
    private final int turnNo;
    private final int attemptNo;
    private final AgentSessionPurpose purpose;
    private final List<Map<String, Object>> steps = new ArrayList<>();

    private AgentStage stage;
    private AgentStageDecision.Reason stageReason;
    private String model;
    private String promptVersion;
    private String policyVersion;
    private AgentTraceOutcome outcome = AgentTraceOutcome.SUCCESS;
    private Long providerDurationMs;
    private String causeType;
    private String downgradePath;
    private String violation;

    public AgentTraceCollector(
            Long sessionId,
            Long userId,
            Long recordId,
            int turnNo,
            int attemptNo,
            AgentSessionPurpose purpose,
            AgentStage stage) {
        this.traceId = UUID.randomUUID().toString().replace("-", "");
        this.sessionId = sessionId;
        this.userId = userId;
        this.recordId = recordId;
        this.turnNo = turnNo;
        this.attemptNo = attemptNo;
        this.purpose = purpose == null ? AgentSessionPurpose.WRITING_GUIDANCE : purpose;
        this.stage = stage;
    }

    // ---------- thought ----------

    /**
     * 本轮所处的模式与轮次预算。
     */
    public AgentTraceCollector mode(AgentChatMode mode, int maxTurns) {
        return step("mode",
                "stageMachineDriven", mode != null && mode.isStageMachineDriven(),
                "toolsAvailable", mode != null && mode.areToolsAvailable(),
                "materialProduced", mode != null && mode.isMaterialProduced(),
                "maxTurns", maxTurns);
    }

    /**
     * 阶段判定结论。
     *
     * 复用既有 {@link AgentStageDecision.Reason}，不为可观测另造一套并行的阶段语义。
     * 回看模式不经阶段机时**不调用本方法**——不伪造一个不存在的判定结论。
     */
    public AgentTraceCollector stageDecision(AgentStage from, AgentStage to, AgentStageDecision.Reason reason) {
        this.stage = to;
        this.stageReason = reason;
        return step("stage-decision",
                "from", from == null ? null : from.name(),
                "to", to == null ? null : to.name(),
                "reason", reason == null ? null : reason.name());
    }

    /**
     * 同轮重试：用户消息已落库，本轮不再推进阶段机。
     */
    public AgentTraceCollector stageRetained(AgentStage current) {
        this.stage = current;
        return step("stage-retained", "stage", current == null ? null : current.name());
    }

    /**
     * 记忆检索的执行情况。
     *
     * 三种状态必须可区分（沿用 C3a「开关关闭不静默」的既有语义）：
     * 开关关闭（{@code enabled=false}）、检索失败（{@code failed=true}）、
     * 检索成功但无命中（{@code enabled=true, failed=false, retrievedCount=0}）。
     * 把它们记成同一种「没有记忆」会让排查时分不清是配置问题、故障还是数据问题。
     */
    public AgentTraceCollector memoryRetrieval(
            boolean enabled, boolean failed, boolean hasCue, int cueCount, int tagCount, int retrievedCount) {
        return step("memory-retrieval",
                "enabled", enabled,
                "failed", failed,
                "hasCue", hasCue,
                "cueCount", cueCount,
                "tagCount", tagCount,
                "retrievedCount", retrievedCount);
    }

    /**
     * 实际注入 prompt 的记忆规模。
     *
     * 与 {@link #memoryRetrieval} 分开的理由：回看模式下被回看记录自身的片段
     * 也进 MEMORY 层（C3b 决策 4），它们**不来自检索**。合成一条会让
     * 「检索命中 0 但注入了 3 条」看起来像自相矛盾。
     */
    public AgentTraceCollector memoryInjected(int injectedCount, int injectedChars) {
        return step("memory-injected",
                "injectedCount", injectedCount,
                "injectedChars", injectedChars);
    }

    /**
     * 上下文组装规模。
     *
     * 只记条数与是否含各补充段，**不记提示词全文**（隐私禁止清单）。
     */
    public AgentTraceCollector prompt(
            int messageCount, boolean toolSupplement, boolean memorySupplement, boolean draftExcerpt) {
        return step("prompt",
                "messageCount", messageCount,
                "toolSupplement", toolSupplement,
                "memorySupplement", memorySupplement,
                "draftExcerpt", draftExcerpt);
    }

    // ---------- action ----------

    /**
     * provider 调用结果。
     *
     * 成功路径同样记录耗时——C5 之前只有失败路径计算耗时，成功路径的起始时间被直接丢弃。
     *
     * @param mocked true 表示走 mock provider，未发生真实外调
     */
    public AgentTraceCollector provider(String model, long durationMs, boolean mocked, boolean success) {
        return provider(AgentProviderPhase.INITIAL, model, durationMs, mocked, success);
    }

    /**
     * C7：同一业务轮可包含 initial 与 reflection 两个 provider 子调用。
     * 顶层耗时按子调用累加，单次耗时与 phase 保留在 steps 中。
     */
    public AgentTraceCollector provider(
            AgentProviderPhase phase, String model, long durationMs, boolean mocked, boolean success) {
        this.model = model;
        this.providerDurationMs = (this.providerDurationMs == null ? 0L : this.providerDurationMs)
                + Math.max(0L, durationMs);
        return step("provider",
                "phase", phase == null ? AgentProviderPhase.INITIAL.id() : phase.id(),
                "model", model,
                "durationMs", durationMs,
                "mocked", mocked,
                "success", success);
    }

    /** C7：记录是否允许进入 reply reflection；只接受结构化枚举与数字。 */
    public AgentTraceCollector reflectionDecision(
            boolean eligible, AgentGuardrailViolation reason, int maxRetries) {
        return step("reflection-decision",
                "path", "reply",
                "eligible", eligible,
                "reason", reason == null ? null : reason.reason(),
                "maxRetries", maxRetries);
    }

    /** C7：记录 reply reflection 的脱敏终态。 */
    public AgentTraceCollector reflectionResult(
            boolean attempted, boolean passed, AgentReflectionTerminal terminal) {
        return step("reflection-result",
                "path", "reply",
                "attempted", attempted,
                "passed", passed,
                "terminal", terminal == null ? null : terminal.id());
    }

    /** Reflection provider 失败只记异常类型，不把整轮误标为 provider initial failure。 */
    public AgentTraceCollector reflectionProviderFailed(Class<?> cause) {
        return step("reflection-provider-failed",
                "causeType", cause == null ? "unknown" : cause.getSimpleName());
    }

    /**
     * provider 失败。只记异常类型，不记异常消息——消息可能回带请求内容。
     */
    public AgentTraceCollector providerFailed(String operation, Class<?> cause) {
        this.outcome = AgentTraceOutcome.FAILED;
        this.causeType = cause == null ? "unknown" : cause.getSimpleName();
        return step("provider-failed", "operation", operation, "causeType", this.causeType);
    }

    /**
     * provider 返回内容无效（非异常，但拿不到可用回复）。
     */
    public AgentTraceCollector providerInvalidContent(String operation) {
        this.outcome = AgentTraceOutcome.FAILED;
        this.causeType = "invalid-content";
        return step("provider-invalid-content", "operation", operation);
    }

    /**
     * provider 未配置或不可用，本轮未发起调用。
     */
    public AgentTraceCollector providerUnavailable() {
        this.outcome = AgentTraceOutcome.UNAVAILABLE;
        this.causeType = "provider-unavailable";
        return step("provider-unavailable");
    }

    /**
     * 工具提议的处置概况。
     */
    public AgentTraceCollector tools(boolean toolsEnabled, int returnedCount, int proposedCount) {
        return step("tools",
                "toolsEnabled", toolsEnabled,
                "returnedCount", returnedCount,
                "proposedCount", proposedCount);
    }

    /**
     * 模型在不下发工具的模式下仍返回了提议，被 fail-closed 丢弃。
     *
     * C3b 归档时该分支未活体触发，正确性仅由单测覆盖。轨迹让它真发生的那一次能被记下。
     */
    public AgentTraceCollector toolsFailClosed(int discardedCount) {
        return step("tools-fail-closed", "discardedCount", discardedCount);
    }

    /**
     * 某条提议被护栏拒绝。
     *
     * @param reasonId 取自 {@code AgentToolValidationResult} 的结构化常量，非自由文本
     */
    public AgentTraceCollector toolRejected(String reasonId) {
        return step("tool-rejected", "reason", reasonId);
    }

    // ---------- observation ----------

    /**
     * 某一层护栏的判定结论与数值指标。
     *
     * {@link AgentGuardrailVerdict} 本身已是脱敏形态（只含比例、长度与违规类型），
     * 可直接落库，不需要第二套摘要机制。
     */
    public AgentTraceCollector guardrail(AgentTraceLayer layer, AgentGuardrailVerdict verdict) {
        if (layer == null || verdict == null) {
            return this;
        }
        return step("guardrail",
                "layer", layer.id(),
                "passed", verdict.isPassed(),
                "violation", verdict.reason(),
                "coverage", round(verdict.coverage()),
                "maxUncoveredRun", verdict.maxUncoveredRun(),
                "checkedLength", verdict.checkedLength());
    }

    /**
     * 一次降级。
     *
     * {@code fallbackLocal} 使兜底回复可与 provider 正常产出区分——
     * 沿用 C4 已接受的条款，不得回退成「降级看起来像一次正常成功」。
     */
    public AgentTraceCollector downgrade(
            AgentTraceLayer layer, AgentGuardrailViolation violation, boolean fallbackLocal) {
        if (layer != null) {
            this.downgradePath = layer.id();
        }
        if (violation != null) {
            this.violation = violation.reason();
        }
        if (this.outcome == AgentTraceOutcome.SUCCESS) {
            this.outcome = AgentTraceOutcome.DOWNGRADED;
        }
        return step("downgrade",
                "layer", layer == null ? null : layer.id(),
                "violation", violation == null ? null : violation.reason(),
                "fallback", fallbackLocal ? "local" : "none");
    }

    /**
     * 回复被长度硬上限裁剪。
     *
     * 不算降级——内容仍是 provider 的产出，只是被截短。但它值得留痕：
     * 排查「Agent 的话怎么断在半句」时，这一条是直接答案。
     */
    public AgentTraceCollector replyClipped(int beforeLength, int afterLength) {
        return step("reply-clipped",
                "layer", AgentTraceLayer.REPLY_LENGTH.id(),
                "beforeLength", beforeLength,
                "afterLength", afterLength);
    }

    /**
     * 素材是否产出。素材被护栏丢弃时 {@code produced=false}。
     */
    public AgentTraceCollector material(boolean produced, int chars) {
        return step("material", "produced", produced, "chars", chars);
    }

    /**
     * 素材生成时 provider 失败。
     *
     * 刻意**不改本轮 outcome**：素材是可选产物，缺失时前端只是不显示回填入口，
     * 对话本身是成功的。把它记成 FAILED 会让「对话成功但素材没出来」
     * 在轨迹里看起来像一轮失败。
     */
    public AgentTraceCollector materialFailed(String causeType) {
        return step("material-failed", "causeType", causeType);
    }

    /**
     * 会话被收束。
     */
    public AgentTraceCollector sessionEnded(String reasonId) {
        return step("session-ended", "reason", reasonId);
    }

    // ---------- 版本锚点 ----------

    public AgentTraceCollector versions(String promptVersion, String policyVersion) {
        this.promptVersion = promptVersion;
        this.policyVersion = policyVersion;
        return this;
    }

    // ---------- 读取 ----------

    public String traceId() {
        return traceId;
    }

    public Long sessionId() {
        return sessionId;
    }

    public Long userId() {
        return userId;
    }

    public Long recordId() {
        return recordId;
    }

    public int turnNo() {
        return turnNo;
    }

    public int attemptNo() {
        return attemptNo;
    }

    public AgentSessionPurpose purpose() {
        return purpose;
    }

    public AgentStage stage() {
        return stage;
    }

    public AgentStageDecision.Reason stageReason() {
        return stageReason;
    }

    public String model() {
        return model;
    }

    public String promptVersion() {
        return promptVersion;
    }

    public String policyVersion() {
        return policyVersion;
    }

    public AgentTraceOutcome outcome() {
        return outcome;
    }

    public Long providerDurationMs() {
        return providerDurationMs;
    }

    public String causeType() {
        return causeType;
    }

    public String downgradePath() {
        return downgradePath;
    }

    public String violation() {
        return violation;
    }

    public List<Map<String, Object>> steps() {
        return List.copyOf(steps);
    }

    // ---------- 内部 ----------

    /**
     * 追加一个步骤。
     *
     * 私有且只由上面的类型化方法调用——这是「不接受任意文本」这条约束的落点：
     * 外部拿不到这个可变参数入口。
     */
    private AgentTraceCollector step(String type, Object... keyValues) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("step", type);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                entry.put(String.valueOf(keyValues[i]), value);
            }
        }
        steps.add(entry);
        return this;
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
