package com.flashback.agent.tool;

import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentSourceCorpus;
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
    private final Clock clock;

    public AgentToolValidator(
            AgentToolRegistry registry,
            AppAgentProperties appAgentProperties,
            AgentFaithfulnessChecker faithfulnessChecker,
            AgentContentChecker contentChecker,
            Clock clock) {
        this.registry = registry;
        this.appAgentProperties = appAgentProperties;
        this.faithfulnessChecker = faithfulnessChecker;
        this.contentChecker = contentChecker;
        this.clock = clock;
    }

    /**
     * 校验一条原始提议。
     *
     * @param wireName 模型给出的工具名
     * @param rawArgs  已解析的参数载体
     * @param hasDraft 当前会话是否绑定了可编辑草稿
     * @param corpus   C4：来源集合（本会话用户原话），用于忠实度判定
     */
    public AgentToolValidationResult validate(
            String wireName, AgentToolRawArguments rawArgs, boolean hasDraft, AgentSourceCorpus corpus) {
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

    private String validateAskText(String askText, AgentSourceCorpus corpus) {
        AgentGuardrailVerdict quoteVerdict = contentChecker.checkQuotes(askText, corpus);
        if (!quoteVerdict.isPassed()) {
            return AgentToolValidationResult.REASON_FABRICATED_QUOTE;
        }
        AgentGuardrailVerdict contentVerdict = contentChecker.check(askText, corpus);
        if (!contentVerdict.isPassed()) {
            return AgentToolValidationResult.REASON_ASK_TEXT_VIOLATION;
        }
        return null;
    }

    private AgentToolValidationResult validateAppendContent(
            String askText, AgentToolRawArguments rawArgs, AgentSourceCorpus corpus) {
        String text = normalize(rawArgs.text());
        if (text == null) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
        }
        // strict mode 不支持 maxLength，长度上限只能在此把关。
        if (text.length() > appAgentProperties.getMaxToolContentChars()) {
            return AgentToolValidationResult.rejected(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
        }
        // C4 核心：这段文字会进入用户日记正文，必须忠实于用户自己说过的话。
        // 判定不通过则提议根本不落库，用户看不到那个确认条。
        AgentGuardrailVerdict verdict = faithfulnessChecker.check(text, corpus);
        if (!verdict.isPassed()) {
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
