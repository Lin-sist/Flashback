package com.flashback.agent.tool;

import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.config.AppAgentProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 提议校验（C2 + C4）。
 *
 * 职责分层（C2 design.md §3.2）：
 * - provider 侧 strict mode 负责**类型与形状**（工具名、参数类型、unlockAt 形状）；
 * - 本类负责 strict mode 无法表达的**业务边界**：字符长度、数量上限、时序、草稿上下文。
 *
 * 因此即便 provider 已做 schema 校验，本类的校验也**不得跳过**——
 * strict mode 是 Beta 且可被配置关闭，白名单的最终强制点始终在后端。
 *
 * C4 增量（C4 design.md 决策 2）：新增**内容忠实度**维度。
 * C2 的三道校验（白名单 / 类型 / 边界）全部只问「能否执行」，
 * 没有任何一层问「这段文字是不是用户说过的」——这正是 R1 穿透两层防御的缝隙。
 * 忠实度闸放在本类而非执行层，是因为 C2 已把「提议是否合法」的全部判断
 * 收敛在这一个校验点上；放在同一处才能保证系统里不存在
 * 「绕过忠实度检查即可产生待确认提议」的路径。
 */
@Component
public class AgentToolValidator {

    private final AgentToolRegistry registry;
    private final AppAgentProperties appAgentProperties;
    private final AgentFaithfulnessChecker faithfulnessChecker;
    private final AgentContentChecker contentChecker;
    private final AgentTimeAttributionChecker timeAttributionChecker;
    private final Clock clock;

    public AgentToolValidator(
            AgentToolRegistry registry,
            AppAgentProperties appAgentProperties,
            AgentFaithfulnessChecker faithfulnessChecker,
            AgentContentChecker contentChecker,
            AgentTimeAttributionChecker timeAttributionChecker,
            Clock clock) {
        this.registry = registry;
        this.appAgentProperties = appAgentProperties;
        this.faithfulnessChecker = faithfulnessChecker;
        this.contentChecker = contentChecker;
        this.timeAttributionChecker = timeAttributionChecker;
        this.clock = clock;
    }

    /**
     * 校验一条原始提议。
     *
     * C3 的来源分层在此体现（design.md §2.4、不变量 2）：
     * - 正文参数只认**会话层**——记忆内容不得成为正文来源；
     * - 提议话术认**两层**，因为它天然可以提起过去的事，但须带时间归属。
     *
     * @param wireName 模型给出的工具名
     * @param rawArgs  已解析的参数载体
     * @param hasDraft 当前会话是否绑定了可编辑草稿
     * @param corpus   分层来源集合
     */
    public AgentToolValidationResult validate(
            String wireName, AgentToolRawArguments rawArgs, boolean hasDraft, AgentSourceCorpus corpus) {
        // C4 签名保留：只有会话层来源时等价于未注入记忆，判定行为与 C4 完全一致。
        // 保留它不是为了兼容测试，而是因为「无记忆」是一个真实且常见的运行状态
        // （检索无命中、检索失败、记忆开关关闭），调用方不该被迫先包一层。
        return validate(wireName, rawArgs, hasDraft, AgentLayeredCorpus.sessionOnly(corpus));
    }

    /**
     * C3 重载：使用分层来源集合校验。
     */
    public AgentToolValidationResult validate(
            String wireName, AgentToolRawArguments rawArgs, boolean hasDraft, AgentLayeredCorpus corpus) {
        if (!registry.isProposable(wireName)) {
            // 白名单外，或模型试图调用后端预注入的读工具。
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        }
        AgentToolName tool = AgentToolName.fromWireName(wireName);
        if (rawArgs == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        if (!hasDraft) {
            // 写工具必须有作用对象；后端不会为了满足工具而创建新记录。
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_NO_DRAFT_CONTEXT);
        }

        String askText = normalize(rawArgs.askText());
        if (askText == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }

        // C4：askText 是唯一显示在确认条上的文本（R1 里它自称「我帮你整理了一下」）。
        // 它天然含 Agent 自己的话，故不做覆盖率判定，只查伪引用与诊断 / 代决表述。
        String askTextRejection = validateAskText(askText, corpus);
        if (askTextRejection != null) {
            return AgentToolValidationResult.rejected(askTextRejection);
        }

        return switch (tool) {
            case APPEND_RECORD_CONTENT -> validateAppendContent(askText, rawArgs, corpus);
            case ADD_RECORD_TAGS -> validateAddTags(askText, rawArgs);
            case PROPOSE_UNLOCK_AT -> validateUnlockAt(askText, rawArgs);
            default -> AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        };
    }

    /**
     * 提议话术的检查。
     *
     * 用**合并层**：话术可以合法地提起过去的事（「我记得你三月份也写过……要不要……」），
     * 若只用会话层，这类正确表述会被判成伪引用。
     * 代价由时间归属检查补上——提起过去必须说清是过去。
     */
    private String validateAskText(String askText, AgentLayeredCorpus corpus) {
        AgentSourceCorpus combined = corpus.combined();
        AgentGuardrailVerdict quoteVerdict = contentChecker.checkQuotes(askText, combined);
        if (!quoteVerdict.isPassed()) {
            return AgentToolValidationResult.REASON_FABRICATED_QUOTE;
        }
        AgentGuardrailVerdict contentVerdict = contentChecker.check(askText, combined);
        if (!contentVerdict.isPassed()) {
            return AgentToolValidationResult.REASON_ASK_TEXT_VIOLATION;
        }
        AgentGuardrailVerdict attributionVerdict = timeAttributionChecker.check(askText, corpus);
        if (!attributionVerdict.isPassed()) {
            return AgentToolValidationResult.REASON_MISSING_TIME_ATTRIBUTION;
        }
        return null;
    }

    private AgentToolValidationResult validateAppendContent(
            String askText, AgentToolRawArguments rawArgs, AgentLayeredCorpus corpus) {
        String text = normalize(rawArgs.text());
        if (text == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        // strict mode 不支持 maxLength，长度上限只能在此把关。
        if (text.length() > appAgentProperties.getMaxToolContentChars()) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
        }
        // C4 核心 + C3 收紧：这段文字会进入用户日记正文，
        // 必须忠实于用户**在本次对话中**说过的话——只认会话层（不变量 2，不可配置）。
        AgentGuardrailVerdict verdict = faithfulnessChecker.check(text, corpus.sessionOnly());
        if (!verdict.isPassed()) {
            // C3：区分「编了一句」与「把旧记录搬过来」。
            // 若这段文字在合并层忠实、只在会话层不忠实，说明它来自注入的记忆。
            if (corpus.hasMemory() && faithfulnessChecker.check(text, corpus.combined()).isPassed()) {
                return AgentToolValidationResult.rejected(
                        AgentToolValidationResult.REASON_MEMORY_AS_CONTENT);
            }
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
        }
        return AgentToolValidationResult.accepted(AgentToolProposal.appendContent(askText, text));
    }

    private AgentToolValidationResult validateAddTags(String askText, AgentToolRawArguments rawArgs) {
        List<Long> rawTagIds = rawArgs.tagIds();
        if (rawTagIds == null || rawTagIds.isEmpty()) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long tagId : rawTagIds) {
            if (tagId == null || tagId <= 0) {
                return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
            }
            deduplicated.add(tagId);
        }
        // strict mode 不支持 maxItems，数量上限只能在此把关。
        if (deduplicated.size() > appAgentProperties.getMaxToolTagIds()) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
        }
        return AgentToolValidationResult.accepted(
                AgentToolProposal.addTags(askText, new ArrayList<>(deduplicated)));
    }

    private AgentToolValidationResult validateUnlockAt(String askText, AgentToolRawArguments rawArgs) {
        String raw = normalize(rawArgs.unlockAt());
        if (raw == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        LocalDateTime parsed = parseUnlockAt(raw);
        if (parsed == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        // 「晚于当前时间」是业务边界，strict mode 的 pattern 只能校验形状。
        if (!parsed.isAfter(LocalDateTime.now(clock))) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
        }
        return AgentToolValidationResult.accepted(
                AgentToolProposal.proposeUnlockAt(askText, parsed.toString()));
    }

    /**
     * 解析 unlockAt。容忍缺省秒的写法（2026-08-01T09:30）。
     */
    public LocalDateTime parseUnlockAt(String raw) {
        String normalized = normalize(raw);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
