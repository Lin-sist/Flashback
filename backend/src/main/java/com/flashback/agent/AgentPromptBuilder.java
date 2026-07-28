package com.flashback.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.agent.guardrail.AgentGuardrailRules;
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

    /** 形状兜底时尝试剥离的字段名，覆盖 C1 遗留约定与常见变体。 */
    private static final List<String> REPLY_FIELD_CANDIDATES = List.of("reply", "askText", "content", "message");

    private static final ObjectMapper SHAPE_MAPPER = new ObjectMapper();

    private final AppAgentProperties appAgentProperties;
    private final AgentGuardrailPolicy guardrailPolicy;
    private final AgentGuardrailRules guardrailRules;

    public AgentPromptBuilder(
            AppAgentProperties appAgentProperties,
            AgentGuardrailPolicy guardrailPolicy,
            AgentGuardrailRules guardrailRules) {
        this.appAgentProperties = appAgentProperties;
        this.guardrailPolicy = guardrailPolicy;
        this.guardrailRules = guardrailRules;
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
        // C2 起走原生 function calling：回复取自 message.content，不再包一层 JSON。
        // 若这里仍要求模型输出 {"reply":...}，模型会照做，而后端不再剥壳，
        // JSON 原文就会直接显示在对话气泡里（C2 手验实际发生过）。
        builder.append("""
                输出要求：直接输出你要对用户说的那句话本身，就像在聊天里说话一样。
                不要输出 JSON、不要加引号包裹、不要写字段名或任何格式标记。
                不要输出分析、标签、评分或诊断。
                """.trim());

        String excerpt = excerptOf(draftExcerpt);
        if (excerpt != null) {
            builder.append("\n\n用户已经写下的正文（只读参考，禁止改写或替换）：\n").append(excerpt);
        }
        return builder.toString();
    }

    /**
     * 形状兜底：若模型仍把回复包成 {"reply":"..."} 之类的 JSON，剥出其中的文本。
     *
     * 边界说明：这是**格式**兜底，不是 C4 的内容合规过滤——
     * 它只处理「模型没听懂输出格式要求」这一种确定性问题，不判断语义、不改写措辞。
     * 保留它的理由：M4 已观察到结构化输出遵从率并非 100%，
     * 且一旦不遵从，用户会直接看到 JSON 原文（C2 手验实际发生过）。
     */
    public String normalizeReplyShape(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return trimmed;
        }
        for (String field : REPLY_FIELD_CANDIDATES) {
            String extracted = extractJsonStringField(trimmed, field);
            if (extracted != null) {
                return extracted;
            }
        }
        return trimmed;
    }

    /**
     * 从 JSON 文本中取出指定字符串字段。解析失败返回 null，由调用方保留原文。
     */
    private String extractJsonStringField(String json, String field) {
        try {
            JsonNode node = SHAPE_MAPPER.readTree(json);
            if (!node.isObject()) {
                return null;
            }
            JsonNode value = node.get(field);
            if (value == null || !value.isTextual()) {
                return null;
            }
            String text = value.asText().trim();
            return text.isEmpty() ? null : text;
        } catch (Exception ex) {
            return null;
        }
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
        // C4：文案取自 AgentGuardrailRules 唯一声明源（design 决策 5），避免与后置检查规则漂移。
        builder.append(guardrailRules.toolUsageClause());

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
        // C4：素材约束文案同样取自唯一声明源（design 决策 5）；文字内容未改动。
        String system = ROLE_SETTING.trim() + "\n\n"
                + guardrailPolicy.guardrailClause() + "\n\n"
                + guardrailRules.materialClause();
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
