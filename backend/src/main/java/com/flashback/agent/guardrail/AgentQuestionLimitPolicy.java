package com.flashback.agent.guardrail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P4.1：回复问题数的确定性后置检查。
 *
 * 中文、英文与混合连续问号视为同一个句末问题；本类只接收候选回复和 0/1 上限，
 * 不做语义分类，也不记录文本内容。
 */
public final class AgentQuestionLimitPolicy {

    private static final Pattern QUESTION_MARK_RUN = Pattern.compile("[?？]+");

    public AgentGuardrailVerdict check(String reply, int maxQuestions) {
        if (maxQuestions < 0 || maxQuestions > 1) {
            throw new IllegalArgumentException("maxQuestions must be 0 or 1");
        }
        if (reply == null || reply.isBlank()) {
            return AgentGuardrailVerdict.pass();
        }
        int questionCount = countQuestions(reply);
        if (questionCount > maxQuestions) {
            return AgentGuardrailVerdict.violation(
                    AgentGuardrailViolation.EXCESSIVE_QUESTIONS,
                    0d,
                    questionCount,
                    reply.length());
        }
        return AgentGuardrailVerdict.pass();
    }

    public int countQuestions(String reply) {
        if (reply == null || reply.isBlank()) {
            return 0;
        }
        int count = 0;
        Matcher matcher = QUESTION_MARK_RUN.matcher(reply);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** 供 trace policy version 覆盖确定性规则。 */
    public static String fingerprintSource() {
        return "question-mark-runs=[?？]+\nallowed-limits=0|1";
    }
}
