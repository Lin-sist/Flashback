package com.flashback.agent;

import com.flashback.config.AppAgentProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C1 最小护栏。
 *
 * 边界（design.md 决策 7）：
 * - 本类只承载 system prompt 级护栏文案 + 回复长度硬裁剪；
 * - C1 不做后置语义过滤、不做违规降级，系统化 hardening 留给 C4；
 * - 长度裁剪是唯一确定性强、不误伤语义的机械约束，因此在 C1 就实现。
 */
@Component
public class AgentGuardrailPolicy {

    /** 句末标点，裁剪时优先在此断开，保持语义完整可读。 */
    private static final List<Character> SENTENCE_ENDINGS = List.of('。', '！', '？', '…', '.', '!', '?', '；', ';');

    /** 五条最小护栏，注入 system prompt。 */
    static final List<String> MINIMUM_GUARDRAILS = List.of(
            "不诊断：不使用任何心理或医学诊断词，不判断病症，不给医学建议。",
            "不覆写：不改写、不替换、不“修正”用户的原话；需要引用时原样引用。",
            "建议不代决：封存、解锁、删除只能建议，由用户自己在页面确认，你不能代替用户决定。",
            "被动陪伴：不主动开启新话题分析用户，不催促，不追问已被回避的问题。",
            "输出克制：一次只说一到两句，不长于用户的表达，不说教、不总结陈词。");

    private final AppAgentProperties appAgentProperties;

    public AgentGuardrailPolicy(AppAgentProperties appAgentProperties) {
        this.appAgentProperties = appAgentProperties;
    }

    /**
     * 护栏条款文本，供 prompt 组装使用。
     */
    public String guardrailClause() {
        StringBuilder builder = new StringBuilder("你必须始终遵守以下边界：\n");
        for (int i = 0; i < MINIMUM_GUARDRAILS.size(); i++) {
            builder.append(i + 1).append(". ").append(MINIMUM_GUARDRAILS.get(i)).append('\n');
        }
        return builder.toString().trim();
    }

    public int maxReplyChars() {
        return appAgentProperties.getMaxReplyChars();
    }

    /**
     * 将回复裁剪到长度上限内，优先在句末标点断开。
     */
    public String enforceReplyLength(String reply) {
        if (reply == null) {
            return null;
        }
        String normalized = reply.trim();
        int limit = maxReplyChars();
        if (normalized.length() <= limit) {
            return normalized;
        }

        String head = normalized.substring(0, limit);
        int cut = lastSentenceEnd(head);
        if (cut > 0) {
            return head.substring(0, cut + 1);
        }
        return head;
    }

    private int lastSentenceEnd(String value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            if (SENTENCE_ENDINGS.contains(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
