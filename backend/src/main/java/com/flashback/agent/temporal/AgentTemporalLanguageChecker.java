package com.flashback.agent.temporal;

import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentGuardrailViolation;

import java.util.List;
import java.util.regex.Pattern;

/** C9 时间解释护栏：阻止频率量化、因果诊断、趋势与预测越界。 */
public class AgentTemporalLanguageChecker {

    private static final Pattern PERCENT_OR_SCORE = Pattern.compile(
            "(?:\\d+(?:\\.\\d+)?%|百分之[零一二三四五六七八九十百]+|\\d+分(?:制|评分|[，。；！？]|$))");
    private static final List<String> OVERREACH = List.of(
            "每次都", "一直都是", "必然", "肯定会", "以后还会", "越来越",
            "形成趋势", "呈现趋势", "形成周期", "固定周期", "形成规律", "存在规律", "有规律",
            "根本原因", "导致了", "说明你有", "证明你", "复发率");

    public AgentGuardrailVerdict check(String candidate) {
        return check(candidate, true);
    }

    public AgentGuardrailVerdict check(String candidate, boolean recurrenceEligible) {
        if (candidate == null || candidate.isBlank()) {
            return AgentGuardrailVerdict.pass();
        }
        int hintCount = occurrences(candidate, "似乎不止一次");
        if ((!recurrenceEligible && hintCount > 0)
                || hintCount > 1
                || PERCENT_OR_SCORE.matcher(candidate).find()
                || OVERREACH.stream().anyMatch(candidate::contains)) {
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.TEMPORAL_OVERREACH);
        }
        return AgentGuardrailVerdict.pass();
    }

    private int occurrences(String value, String token) {
        int count = 0;
        for (int from = 0; (from = value.indexOf(token, from)) >= 0; from += token.length()) {
            count++;
        }
        return count;
    }

    public String fingerprintSource() {
        return "temporal-language:v1:" + PERCENT_OR_SCORE.pattern() + ':' + String.join("|", OVERREACH);
    }
}
