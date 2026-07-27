package com.flashback.agent;

import com.flashback.agent.tool.AgentToolCallStatus;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentToolCall;
import com.flashback.vo.TagVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 组装 Agent 对话的 provider 请求 messages。
 *
 * 约定：
 * - system 段固定包含角色设定 + 五条最小护栏 + 长度上限 + 当前阶段目标；
 * - 历史消息按滑动窗口截取，C1 不做摘要压缩（留给 C3 Memory）；
 * - 草稿正文只作为只读引用注入，禁止让模型改写。
 */
@Component
public class AgentPromptBuilder {

    private static final String ROLE_SETTING = """
            你是《时光回序》里的一个朋友。用户在这里写下当下的情绪、困惑、期待与生活片段。
            你的气质是安静、私密、克制、温柔：不热情也不冷漠，用户找你时你就在。
            你的任务是用温和的提问，帮用户把此刻的感受一点点说出来，而不是替他写、替他总结、替他决定。
            """;

    private final AppAgentProperties appAgentProperties;
    private final AgentGuardrailPolicy guardrailPolicy;

    public AgentPromptBuilder(AppAgentProperties appAgentProperties, AgentGuardrailPolicy guardrailPolicy) {
        this.appAgentProperties = appAgentProperties;
        this.guardrailPolicy = guardrailPolicy;
    }

    /**
     * 组装一次对话调用的 messages。
     *
     * @param targetStage  本轮 Agent 需要承担的阶段目标
     * @param history      会话历史消息（正序）
     * @param draftExcerpt 当前草稿正文摘录，可为 null
     */
    public List<Map<String, String>> buildConversationMessages(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt) {
        return buildConversationMessages(targetStage, history, draftExcerpt, null);
    }

    /**
     * C2 重载：附带工具补充上下文。
     *
     * @param toolSupplement 预注入的读工具内容与最近工具执行结果摘要，可为 null
     */
    public List<Map<String, String>> buildConversationMessages(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt,
            String toolSupplement) {
        List<Map<String, String>> messages = new ArrayList<>();
        String system = buildSystemPrompt(targetStage, draftExcerpt);
        if (toolSupplement != null && !toolSupplement.isBlank()) {
            system = system + "\n\n" + toolSupplement.trim();
        }
        messages.add(Map.of("role", "system", "content", system));

        for (AgentMessage message : windowOf(history)) {
            messages.add(Map.of(
                    "role", message.getRole() == AgentMessageRole.ASSISTANT ? "assistant" : "user",
                    "content", message.getContent()));
        }

        messages.add(Map.of("role", "user", "content", buildTurnInstruction(targetStage)));
        return List.copyOf(messages);
    }

    String buildSystemPrompt(AgentStage targetStage, String draftExcerpt) {
        StringBuilder builder = new StringBuilder();
        builder.append(ROLE_SETTING.trim()).append("\n\n");
        builder.append(guardrailPolicy.guardrailClause()).append("\n\n");
        builder.append("回复长度硬上限：").append(guardrailPolicy.maxReplyChars()).append(" 个字符以内。\n\n");
        builder.append("当前引导目标：").append(stageGoal(targetStage)).append("\n\n");
        builder.append("""
                输出要求：只输出 JSON，格式为 {"reply":"你要说的话"}。
                JSON 之外不要输出任何文本。reply 中不要包含分析、标签、评分或诊断。
                """.trim());

        String excerpt = excerptOf(draftExcerpt);
        if (excerpt != null) {
            builder.append("\n\n用户已经写下的正文（只读参考，禁止改写或替换）：\n").append(excerpt);
        }
        return builder.toString();
    }

    /**
     * C2：构造工具补充上下文。
     *
     * 两部分（design §3.1、数据流 2.3）：
     * 1. 预注入读工具内容——可选标签清单。读工具不作为 FC tool 下发，
     * 因为 C2 不做单轮内 FC 循环，模型无法先调工具再提议。
     * 2. 最近工具执行结果的结构化摘要，使 Agent 知道「刚刚做了什么」，
     * 不重复提议同一个已完成的行动。摘要不含日记原文。
     *
     * 同时给出工具使用的气质约束：提议而非代决，且不得改写用户原文。
     */
    public String buildToolSupplement(List<TagVO> availableTags, List<AgentToolCall> recentToolCalls) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                关于你可以做的小动作：
                - 你可以在合适的时候提议帮用户做一件小事（把他说过的话整理进正文、加个标签、设一个解锁时间）。
                - 提议就只是提议：由用户点确认才会发生，你不能替他决定，也不要反复追问同一件事。
                - 封存、解锁、删除这些事你做不了，只能建议用户自己在页面上确认。
                - 你永远不能改写、替换或“修正”用户已经写下的文字，只能追加他自己说过的内容。
                """.trim());

        if (availableTags != null && !availableTags.isEmpty()) {
            builder.append("\n\n可选标签（只能从这里挑，不能新建）：\n");
            for (TagVO tag : availableTags) {
                builder.append("- id=").append(tag.getId()).append(" ").append(tag.getName()).append('\n');
            }
        }

        if (recentToolCalls != null && !recentToolCalls.isEmpty()) {
            builder.append("\n已经发生过的操作（不要重复提议）：\n");
            for (AgentToolCall toolCall : recentToolCalls) {
                builder.append("- ")
                        .append(toolCall.getToolName())
                        .append('：')
                        .append(toolCall.getStatus() == AgentToolCallStatus.EXECUTED ? "已完成" : "没有成功")
                        .append('\n');
            }
        }
        return builder.toString().trim();
    }

    /**
     * 收束阶段的素材整理请求。素材只能由用户已说过的内容整理而来。
     */
    public List<Map<String, String>> buildMaterialMessages(List<AgentMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        String system = ROLE_SETTING.trim() + "\n\n"
                + guardrailPolicy.guardrailClause() + "\n\n"
                + """
                        现在请把这段对话中【用户自己说过的内容】整理成一小段可以放进记录正文的素材。
                        硬性要求：
                        - 只使用用户说过的内容，不添加你的分析、评价、建议或诊断；
                        - 不改变用户的意思，尽量保留用户自己的措辞；
                        - 语气安静克制，不要写成总结报告；
                        - 只输出 JSON，格式为 {"material":"整理后的素材"}，JSON 之外不要有任何文本。
                        """.trim();
        messages.add(Map.of("role", "system", "content", system));

        // 素材必须覆盖用户在整段会话里说过的话；会话最多 8 轮，全部用户消息可控。
        // 不带 assistant 消息，避免 Agent 自己的表达混入候选正文。
        if (history != null) {
            for (AgentMessage message : history) {
                if (message.getRole() == AgentMessageRole.USER) {
                    messages.add(Map.of("role", "user", "content", message.getContent()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", "请整理素材。"));
        return List.copyOf(messages);
    }

    private String buildTurnInstruction(AgentStage targetStage) {
        return switch (targetStage) {
            case CLOSING -> "请温和地收束这次对话，让用户知道说到这里已经够了，不要再提新问题。";
            default -> "请基于上面的对话，围绕当前引导目标问一个具体、温和、好回答的问题。";
        };
    }

    private String stageGoal(AgentStage targetStage) {
        return switch (targetStage) {
            case OPENING, EMOTION -> "让用户说出此刻最明显的感受是什么，不要问抽象的大问题。";
            case CONFUSION -> "在用户已说出的感受基础上，帮他指出让他卡住的具体地方。";
            case CORE_QUESTION -> "帮用户把困惑收敛成一个他真正想弄明白的问题。";
            case EXPECTATION -> "问用户希望接下来变成什么样，或者希望未来的自己怎么看这一刻。";
            case CLOSING -> "温和收束，不再追问，让用户知道可以停在这里。";
            case ENDED -> "对话已结束，不要再发问。";
        };
    }

    private List<AgentMessage> windowOf(List<AgentMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int window = appAgentProperties.getContextMessageWindow();
        if (history.size() <= window) {
            return history;
        }
        return history.subList(history.size() - window, history.size());
    }

    private String excerptOf(String draftExcerpt) {
        if (draftExcerpt == null) {
            return null;
        }
        String normalized = draftExcerpt.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        int limit = appAgentProperties.getDraftExcerptChars();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
    }
}
