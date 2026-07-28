package com.flashback.agent;

import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.config.AppAgentProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 护栏策略入口（C1 + C4）。
 *
 * C1 边界：本类只承载 system prompt 级护栏文案 + 回复长度硬裁剪；
 * 长度裁剪是唯一确定性强、不误伤语义的机械约束，因此在 C1 就实现。
 *
 * C4 变化（design.md 决策 5）：护栏规则的**声明**迁移到 AgentGuardrailRules
 * 作为唯一事实源，本类的 guardrailClause() 改为委托它。
 * 迁移目的是防漂移——prompt 文案与后置检查规则必须派生自同一份声明，
 * 否则会出现「prompt 允许但检查拦」或「检查放过但 prompt 禁止」的自相矛盾状态。
 *
 * enforceReplyLength 的行为**未改动**：它是 C1 已接受的护栏，
 * 且在多层叠加后仍必须生效（agent-runtime delta 的长度上限条款）。
 */
@Component
public class AgentGuardrailPolicy {

    /** 句末标点，裁剪时优先在此断开，保持语义完整可读。 */
    private static final List<Character> SENTENCE_ENDINGS = List.of('。', '！', '？', '…', '.', '!', '?', '；', ';');

    /**
     * 五条最小护栏。
     *
     * @deprecated C4 起以 {@link AgentGuardrailRules#MINIMUM_GUARDRAILS} 为唯一声明源，
     *             本常量仅为兼容既有引用而保留同一份数据。
     */
    @Deprecated(since = "C4")
    static final List<String> MINIMUM_GUARDRAILS = AgentGuardrailRules.MINIMUM_GUARDRAILS;

    private final AppAgentProperties appAgentProperties;
    private final AgentGuardrailRules guardrailRules;

    public AgentGuardrailPolicy(AppAgentProperties appAgentProperties, AgentGuardrailRules guardrailRules) {
        this.appAgentProperties = appAgentProperties;
        this.guardrailRules = guardrailRules;
    }

    /**
     * 护栏条款文本，供 prompt 组装使用。C4 起委托唯一声明源。
     */
    public String guardrailClause() {
        return guardrailRules.guardrailClause();
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
