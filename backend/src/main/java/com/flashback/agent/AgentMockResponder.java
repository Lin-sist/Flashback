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
        return switch (targetStage) {
            case OPENING, EMOTION -> firstTurn(userInput)
                    ? "今天是什么让你想写下这一刻？"
                    : "这种感觉是从什么时候开始的？";
            case CONFUSION -> "让你卡住的，是具体某件事，还是那种一直压着的感觉？";
            case CORE_QUESTION -> "如果只留一个最想弄明白的问题，你会怎么问它？";
            case EXPECTATION -> "你希望接下来变成什么样？";
            case CLOSING -> "好的，这些已经很好了。";
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

    private boolean firstTurn(String userInput) {
        return userInput == null || userInput.isBlank();
    }
}
