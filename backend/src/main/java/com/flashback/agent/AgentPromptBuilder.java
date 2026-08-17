package com.flashback.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.temporal.TemporalDistanceBand;
import com.flashback.agent.temporal.TemporalMemoryContext;
import com.flashback.agent.temporal.TemporalPolicyResult;
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
 * - system 段固定包含 witness 角色设定 + 五条最小护栏 + 长度上限 + typed turn 边界；
 * - 历史消息按滑动窗口截取，C1 不做摘要压缩（留给 C3 Memory）；
 * - 草稿正文只作为只读引用注入，禁止让模型改写。
 */
@Component
public class AgentPromptBuilder {

    private static final String ROLE_SETTING = """
            你是《时光回序》里有温度的见证者。你在场，但不替用户解释、决定或完成表达。
            先回应用户实际说出的内容；理解不确定时可以说明“我可能理解得不完全”，把修正权留给用户。
            不自称朋友或伴侣，不承诺一直陪伴、主动关心或最懂用户；不把一次表达固化成人格、人生阶段或诊断。
            不强制用户谈情绪、困惑、核心问题或未来期待，不要求用户得出结论。
            """;

    /**
     * 输出格式要求。
     *
     * C5：由 buildSystemPrompt 内联的文本块提取为常量，**文字逐字未改**。
     * 提取的唯一目的是让它能被提示词版本指纹覆盖——留在方法体内则改了它版本号不会变，
     * 那正是决策 6 要避免的脏版本。
     */
    private static final String OUTPUT_REQUIREMENT = """
            输出要求：直接输出你要对用户说的那句话本身，就像在聊天里说话一样。
            不要输出 JSON、不要加引号包裹、不要写字段名或任何格式标记。
            不要输出分析、标签、评分或诊断。
            """;

    /** 草稿正文只读引用的标签文案。C5 同上：提取为常量以纳入版本指纹，文字未改。 */
    private static final String DRAFT_EXCERPT_LABEL = "用户已经写下的正文（只读参考，禁止改写或替换）：";

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
        return buildConversationMessages(targetStage, history, draftExcerpt, toolSupplement, null);
    }

    /**
     * C3 重载：附带记忆补充上下文。
     *
     * 沿用 C2 的 system 段追加位形态，不改既有重载的语义——
     * 既有调用方无需改动，这是 C3 选择「新增追加位」而非「重构组装流程」的直接收益。
     *
     * @param memorySupplement 本轮实际注入的历史记忆片段段落，可为 null
     */
    public List<Map<String, String>> buildConversationMessages(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt,
            String toolSupplement,
            String memorySupplement) {
        return buildConversationMessages(
                targetStage,
                AgentWitnessTurnDirective.safeDefault(targetStage),
                history,
                draftExcerpt,
                toolSupplement,
                memorySupplement);
    }

    /** P4.1：生产编排显式传入后端计算的 typed turn contract。 */
    public List<Map<String, String>> buildConversationMessages(
            AgentStage targetStage,
            AgentWitnessTurnDirective directive,
            List<AgentMessage> history,
            String draftExcerpt,
            String toolSupplement,
            String memorySupplement) {
        List<Map<String, String>> messages = new ArrayList<>();
        AgentWitnessTurnDirective effective = directive == null
                ? AgentWitnessTurnDirective.safeDefault(targetStage)
                : directive;
        String system = buildSystemPrompt(targetStage, effective, draftExcerpt);
        if (toolSupplement != null && !toolSupplement.isBlank()) {
            system = system + "\n\n" + toolSupplement.trim();
        }
        if (memorySupplement != null && !memorySupplement.isBlank()) {
            system = system + "\n\n" + memorySupplement.trim();
        }
        messages.add(Map.of("role", "system", "content", system));

        for (AgentMessage message : windowOf(history)) {
            messages.add(Map.of(
                    "role", message.getRole() == AgentMessageRole.ASSISTANT ? "assistant" : "user",
                    "content", message.getContent()));
        }

        messages.add(Map.of("role", "user", "content", buildTurnInstruction(effective)));
        return List.copyOf(messages);
    }

    String buildSystemPrompt(AgentStage targetStage, String draftExcerpt) {
        return buildSystemPrompt(
                targetStage, AgentWitnessTurnDirective.safeDefault(targetStage), draftExcerpt);
    }

    String buildSystemPrompt(
            AgentStage targetStage,
            AgentWitnessTurnDirective directive,
            String draftExcerpt) {
        StringBuilder builder = new StringBuilder();
        builder.append(ROLE_SETTING.trim()).append("\n\n");
        builder.append(guardrailPolicy.guardrailClause()).append("\n\n");
        builder.append("回复长度硬上限：").append(guardrailPolicy.maxReplyChars()).append(" 个字符以内。\n\n");
        builder.append("本轮回复边界：").append(buildTurnInstruction(directive)).append("\n\n");
        // C2 起走原生 function calling：回复取自 message.content，不再包一层 JSON。
        // 若这里仍要求模型输出 {"reply":...}，模型会照做，而后端不再剥壳，
        // JSON 原文就会直接显示在对话气泡里（C2 手验实际发生过）。
        builder.append(OUTPUT_REQUIREMENT.trim());

        String excerpt = excerptOf(draftExcerpt);
        if (excerpt != null) {
            builder.append("\n\n").append(DRAFT_EXCERPT_LABEL).append('\n').append(excerpt);
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
     * C3：构造记忆补充上下文。
     *
     * 三条约束都写在文本里，且都有代码层的对应硬拦（design.md §2.2）：
     * 1. 每个片段带可读时间锚点 —— 它是时间归属护栏的语义目标；
     * 2. 明写「不是正文素材」 —— 对应「正文只认会话层」的不可配置硬约束；
     * 3. 约束文案取自 AgentGuardrailRules 单一声明源（C4 决策 5 的既有约定），
     * 不在本类内联新规则，避免 prompt 与后置检查随时间分叉。
     *
     * 无片段时返回空串：**不注入占位段**。
     * 一个说「你没有相关的旧事」的段落会诱导模型解释为什么没有，
     * 而产品要的是无命中时安静地不提。
     *
     * @param fragments 本轮实际要注入的片段；调用方须传入与来源集合完全相同的列表
     */
    public String buildMemorySupplement(List<MemoryFragment> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(guardrailRules.memoryUsageClause());
        builder.append("\n\n他以前写下的片段：\n");
        for (MemoryFragment fragment : fragments) {
            if (fragment == null || fragment.text() == null || fragment.text().isBlank()) {
                continue;
            }
            builder.append("- [")
                    .append(fragment.timeLabel())
                    .append('·')
                    .append("记录")
                    .append(fragment.recordId())
                    .append("] ")
                    .append(fragment.text().trim())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    /**
     * C9：把内部时间距离转成克制的理解提示；不输出内部 band 名、阈值或分数。
     */
    public String buildTemporalSupplement(TemporalPolicyResult result) {
        if (result == null || !result.enabled() || result.contexts().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("理解这些过去片段时，请保留时间距离：\n");
        for (TemporalMemoryContext context : result.contexts()) {
            builder.append("- [")
                    .append(context.timeLabel())
                    .append('·').append("记录").append(context.recordId()).append("] ")
                    .append(distanceWording(context.band()))
                    .append("；不要把它说成用户此刻的确定状态。\n");
        }
        if (result.patternEvidence().eligible()) {
            builder.append("若确有必要，最多提示一次“似乎不止一次”，并邀请用户自己判断；")
                    .append("不得扩写成规律、原因、诊断、趋势、分数或预测。\n");
        } else {
            builder.append("不要据此概括反复模式。\n");
        }
        return builder.toString().trim();
    }

    private String distanceWording(TemporalDistanceBand band) {
        return switch (band) {
            case RECENT -> "这是离现在较近的一段";
            case DISTANT -> "这已隔了一段时间，只作辅助参照";
            case LONG_AGO -> "这已过去较久，只能轻触，不能替用户下结论";
            case UNKNOWN -> "时间距离无法可靠判断，不要自行推断";
        };
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

    /**
     * C5：提示词版本指纹的原料。
     *
     * 只读、不改变任何组装行为——它把本类里所有会影响模型输入的**文案**拼在一起，
     * 供 {@code AgentTraceVersions} 派生版本号。
     *
     * 为什么不直接哈希某一次组装好的 system prompt：那份文本里混着草稿摘录、
     * 标签清单、记忆片段等**随会话变化**的内容，哈希会每轮都不同，
     * 版本号就失去了「同一版提示词」的含义。这里只取常量文案。
     *
     * 维护约定：**新增或修改任何会进入 prompt 的常量文案时，须一并列进本方法**，
     * 否则版本号会在文案已变时保持不变——那正是决策 6 要避免的脏版本。
     */
    public String promptTemplateFingerprintSource() {
        StringBuilder builder = new StringBuilder();
        builder.append(ROLE_SETTING).append('\n');
        for (AgentWitnessTurnType type : AgentWitnessTurnType.values()) {
            AgentWitnessTurnDirective directive = switch (type) {
                case REFLECT_ONLY -> AgentWitnessTurnDirective.reflectOnly(AgentStage.WITNESS);
                case MAY_ASK_ONE -> AgentWitnessTurnDirective.mayAskOne(AgentStage.WITNESS);
                case CLOSE -> AgentWitnessTurnDirective.close(
                        AgentStage.CLOSING, AgentStageDecision.Reason.CLOSED);
            };
            builder.append(type.name()).append('>').append(buildTurnInstruction(directive)).append('\n');
        }
        builder.append(OUTPUT_REQUIREMENT).append('\n');
        builder.append(DRAFT_EXCERPT_LABEL).append('\n');
        builder.append(String.join("|", REPLY_FIELD_CANDIDATES)).append('\n');
        return builder.toString();
    }

    private String buildTurnInstruction(AgentWitnessTurnDirective directive) {
        return switch (directive.type()) {
            case REFLECT_ONLY -> "回应已经听见的内容，留出继续或停下的空间，不提问题。";
            case MAY_ASK_ONE -> "先回应，再至多问一个具体且可跳过的问题；没有必要就不问。";
            case CLOSE -> "温和收束，不挽留，不要求结论，也不提新问题。";
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
