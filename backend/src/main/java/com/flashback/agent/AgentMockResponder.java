package com.flashback.agent;

import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * mock provider 下的本地引导器。
 *
 * 边界说明：
 * - 只在 app.ai.provider=mock 且显式启用 mock 时使用，返回结果的 source 始终标记为 mock；
 * - 真实 provider 路径失败时不会走到这里（design.md 决策 5：C1 不引入 FALLBACK），
 * 因此不存在用本地文案冒充真实 provider 成功的情况。
 */
@Component
public class AgentMockResponder {

    public String reply(AgentStage targetStage, String userInput) {
        return reply(targetStage, AgentWitnessTurnDirective.safeDefault(targetStage), userInput);
    }

    public String reply(
            AgentStage targetStage, AgentWitnessTurnDirective directive, String userInput) {
        AgentWitnessTurnDirective effective = directive == null
                ? AgentWitnessTurnDirective.safeDefault(targetStage)
                : directive;
        if (effective.type() == AgentWitnessTurnType.CLOSE) {
            return "好的，说到这里已经很好了。";
        }
        return switch (targetStage) {
            case WITNESS -> effective.type() == AgentWitnessTurnType.MAY_ASK_ONE
                    ? (firstTurn(userInput)
                    ? "我在听。如果你愿意，可以从此刻最想理清的那一部分说起？"
                    : "我听见这件事正占着你的心思。如果只理一小步，你想先看哪一部分？")
                    : (firstTurn(userInput)
                    ? "我在这里，你可以按自己的节奏说。"
                    : "我听见了，你可以继续，也可以停在这里。");
            case OPENING, EMOTION -> firstTurn(userInput)
                    ? "今天是什么让你想写下这一刻？"
                    : "这种感觉是从什么时候开始的？";
            case CONFUSION -> "让你卡住的，是具体某件事，还是那种一直压着的感觉？";
            case CORE_QUESTION -> "如果只留一个最想弄明白的问题，你会怎么问它？";
            case EXPECTATION -> "你希望接下来变成什么样？";
            case CLOSING -> "好的，这些已经很好了。";
            // C3b：回看没有阶段推进，开场先接住「那时候」，之后顺着用户往下聊。
            // 刻意带上时间指示语，使 mock 路径也符合时间归属护栏的要求。
            case REVIEW -> firstTurn(userInput)
                    ? "那时候你写下这些的时候，一定挺不容易的。现在回过头看，你怎么想？"
                    : "那时候的你大概也没想到会走到今天吧。";
            case ENDED -> "这次就聊到这里。";
        };
    }

    /**
     * 由用户已说过的内容整理素材：只拼接用户发言，不添加分析或评价。
     */
    public String material(List<AgentMessage> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (AgentMessage message : history) {
            if (message.getRole() != AgentMessageRole.USER) {
                continue;
            }
            String content = message.getContent() == null ? "" : message.getContent().trim();
            if (content.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(content);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /**
     * C2：mock provider 下伪造原生 tool_calls，供零外调端到端测试。
     *
     * 形状与真实 provider 一致（function name + arguments JSON 字符串），
     * 因此走的是同一条解析与校验路径——测试覆盖的不是一条特设分支。
     *
     * 触发时机刻意保守：只在 CORE_QUESTION 阶段提议一次追加正文，
     * 避免 mock 下每轮都弹确认条影响其他测试。
     */
    public List<AgentRawToolCall> toolCalls(AgentStage targetStage, String userInput, boolean toolsEnabled) {
        return toolCalls(
                targetStage, AgentWitnessTurnDirective.safeDefault(targetStage), userInput, toolsEnabled);
    }

    public List<AgentRawToolCall> toolCalls(
            AgentStage targetStage,
            AgentWitnessTurnDirective directive,
            String userInput,
            boolean toolsEnabled) {
        boolean legacyProposalStage = targetStage == AgentStage.CORE_QUESTION;
        boolean witnessProposalTurn = targetStage == AgentStage.WITNESS
                && directive != null
                && directive.type() == AgentWitnessTurnType.MAY_ASK_ONE;
        if (!toolsEnabled || (!legacyProposalStage && !witnessProposalTurn)) {
            return List.of();
        }
        String material = userInput == null || userInput.isBlank() ? "先记下此刻的感受" : userInput.trim();
        String arguments = "{\"text\":\"" + escapeJson(material)
                + "\",\"askText\":\"要不要把这段放进正文？\"}";
        return List.of(new AgentRawToolCall("append_record_content", arguments));
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean firstTurn(String userInput) {
        return userInput == null || userInput.isBlank();
    }
}
