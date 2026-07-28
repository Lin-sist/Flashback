package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 后置内容检查：诊断与代决（C4）。
 *
 * 关键设计（design.md 决策 4）：规则**只在候选文本的「新增区段」匹配**，
 * 不在全文匹配。理由是用户自己说「我可能有点焦虑症」时，
 * Agent 共情复述带上这个词是蓝图 §6.4 明确要求的恰当行为；
 * 若全文匹配，这类正常回应会被判违规，Agent 会被逼成
 * 「用户一提病名就换话题」，正好掉进蓝图 C4 风险栏点名的
 * 「过度限制导致 Agent 无话可说」。
 *
 * 区分「谁说的」比区分「说了什么词」更接近真实护栏意图：
 * 产品禁止的是 **Agent 下诊断**，不是禁止病名这个字符串出现。
 *
 * 分区能力复用忠实度的覆盖标记，不需要第二套机制。
 */
@Component
public class AgentContentChecker {

    private static final Logger log = LoggerFactory.getLogger(AgentContentChecker.class);

    /** 匹配中英文引号包裹的片段，用于 askText 的伪引用检查。 */
    private static final Pattern QUOTED = Pattern.compile("[“\"「『]([^”\"」』]{2,})[”\"」』]");

    private final AppAgentProperties appAgentProperties;
    private final AgentFaithfulnessChecker faithfulnessChecker;

    public AgentContentChecker(
            AppAgentProperties appAgentProperties, AgentFaithfulnessChecker faithfulnessChecker) {
        this.appAgentProperties = appAgentProperties;
        this.faithfulnessChecker = faithfulnessChecker;
    }

    /**
     * 检查一段 Agent 表述是否越界。
     *
     * fail-closed：判定过程异常时返回违规，不放行未检文本。
     *
     * @param text   Agent 的表述（回复或提议话术）
     * @param corpus 来源集合，用于区分「用户说过的」与「Agent 新增的」
     */
    public AgentGuardrailVerdict check(String text, AgentSourceCorpus corpus) {
        if (!appAgentProperties.getGuardrail().isContentCheckEnabled()) {
            log.info("agent guardrail content check disabled by config");
            return AgentGuardrailVerdict.pass();
        }
        try {
            return evaluate(text, corpus);
        } catch (RuntimeException ex) {
            log.warn("agent guardrail content check failed cause={}", ex.getClass().getSimpleName());
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.CHECK_ERROR);
        }
    }

    /**
     * 伪引用检查：提议话术中引号包裹、声称来自用户的片段必须在来源中有覆盖。
     *
     * 存在理由（proposal §2.4、design.md §2.4）：askText 是唯一显示在确认条上的文本。
     * R1 里它自称「我帮你把这两句整理了一下」——这类表述本身不违规，
     * 但一旦它引号引用了用户「说过的话」而用户没说过，
     * 就是在确认入口上直接展示虚构内容。
     */
    public AgentGuardrailVerdict checkQuotes(String text, AgentSourceCorpus corpus) {
        if (text == null || text.isBlank() || corpus == null || corpus.isEmpty()) {
            return AgentGuardrailVerdict.pass();
        }
        try {
            Matcher matcher = QUOTED.matcher(text);
            while (matcher.find()) {
                String quoted = matcher.group(1);
                // 用更严的引用判据：引用声称是逐字原话，且实测发现伪造的短引用
                // 会落在通用判据的最短受检长度之下而漏放。
                AgentGuardrailVerdict verdict = faithfulnessChecker.checkQuotedFragment(quoted, corpus);
                if (!verdict.isPassed()) {
                    return AgentGuardrailVerdict.violation(
                            AgentGuardrailViolation.FABRICATED_QUOTE,
                            verdict.coverage(),
                            verdict.maxUncoveredRun(),
                            verdict.checkedLength());
                }
            }
            return AgentGuardrailVerdict.pass();
        } catch (RuntimeException ex) {
            log.warn("agent guardrail quote check failed cause={}", ex.getClass().getSimpleName());
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.FABRICATED_QUOTE);
        }
    }

    private AgentGuardrailVerdict evaluate(String text, AgentSourceCorpus corpus) {
        String normalized = AgentTextNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            return AgentGuardrailVerdict.pass();
        }
        AgentCoverageProfile profile = faithfulnessChecker.profileOf(text, corpus);

        // 代决类：谎报已执行不可逆操作。这类表述必然是 Agent 自己说的，
        // 但仍走同一分区逻辑，避免用户复述「你刚才说已经帮我封存了」被判违规。
        AgentGuardrailVerdict fakeAction = matchInAddedRegions(
                normalized, profile, AgentGuardrailRules.FAKE_ACTION_PATTERNS,
                AgentGuardrailViolation.FAKE_ACTION);
        if (!fakeAction.isPassed()) {
            return fakeAction;
        }

        // 诊断类：病症名与判定式表述分开声明，任一落在新增区段即命中。
        AgentGuardrailVerdict diagnostic = matchInAddedRegions(
                normalized, profile, AgentGuardrailRules.DIAGNOSTIC_TERMS,
                AgentGuardrailViolation.DIAGNOSTIC);
        if (!diagnostic.isPassed()) {
            return diagnostic;
        }
        return matchInAddedRegions(
                normalized, profile, AgentGuardrailRules.DIAGNOSTIC_PATTERNS,
                AgentGuardrailViolation.DIAGNOSTIC);
    }

    /**
     * 在「新增区段」内匹配规则词。
     *
     * 实现要点：规则词也要归一化后再匹配，否则规则里的标点会与归一化文本对不上。
     */
    private AgentGuardrailVerdict matchInAddedRegions(
            String normalizedText,
            AgentCoverageProfile profile,
            List<String> rules,
            AgentGuardrailViolation violation) {

        for (String rule : rules) {
            String normalizedRule = AgentTextNormalizer.normalize(rule);
            if (normalizedRule.isEmpty()) {
                continue;
            }
            int from = 0;
            while (true) {
                int hit = normalizedText.indexOf(normalizedRule, from);
                if (hit < 0) {
                    break;
                }
                if (profile.isMostlyUncovered(hit, hit + normalizedRule.length())) {
                    // 命中处主要由 Agent 新增 → 越界。
                    return AgentGuardrailVerdict.violation(
                            violation,
                            profile.coverage(),
                            profile.maxUncoveredRun(),
                            profile.length());
                }
                from = hit + 1;
            }
        }
        return AgentGuardrailVerdict.pass();
    }
}
