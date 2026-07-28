package com.flashback.agent;

import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPromptBuilderTest {

    private AppAgentProperties properties;
    private AgentPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        properties.setMaxReplyChars(120);
        properties.setContextMessageWindow(4);
        properties.setDraftExcerptChars(20);
        // C4：护栏与工具/素材文案统一取自 AgentGuardrailRules 单一声明源（design 决策 5）。
        AgentGuardrailRules rules = new AgentGuardrailRules();
        promptBuilder = new AgentPromptBuilder(
                properties, new AgentGuardrailPolicy(properties, rules), rules);
    }

    @Test
    void shouldEmbedAllMinimumGuardrailsAndLengthLimitInSystemPrompt() {
        String systemPrompt = promptBuilder.buildSystemPrompt(AgentStage.EMOTION, null);

        assertThat(systemPrompt).contains("不诊断", "不覆写", "建议不代决", "被动陪伴", "输出克制");
        assertThat(systemPrompt).contains("120 个字符以内");
    }

    /**
     * C2 起对话回复取自 message.content，不再包 JSON。
     * 回归守门：若 prompt 又要求模型输出 JSON，模型会照做而后端不再剥壳，
     * {"reply":"..."} 原文会直接进入对话气泡（C2 手验实际发生过）。
     */
    @Test
    void shouldNotAskModelForJsonWrappedReply() {
        String systemPrompt = promptBuilder.buildSystemPrompt(AgentStage.EMOTION, null);

        assertThat(systemPrompt).doesNotContain("只输出 JSON");
        assertThat(systemPrompt).doesNotContain("{\"reply\"");
        assertThat(systemPrompt).contains("不要输出 JSON");
    }

    @Test
    void shouldStripJsonWrapperWhenModelIgnoresFormatInstruction() {
        assertThat(promptBuilder.normalizeReplyShape("{\"reply\":\"今天是什么让你想写下这一刻？\"}"))
                .isEqualTo("今天是什么让你想写下这一刻？");
        assertThat(promptBuilder.normalizeReplyShape("  {\"reply\":\"嗯，说到这里已经很好。\"}  "))
                .isEqualTo("嗯，说到这里已经很好。");
    }

    @Test
    void shouldKeepPlainTextReplyUnchanged() {
        assertThat(promptBuilder.normalizeReplyShape("今天是什么让你想写下这一刻？"))
                .isEqualTo("今天是什么让你想写下这一刻？");
        // 正常口语中出现花括号的极端情况：无法解析则原样保留，不误伤内容。
        assertThat(promptBuilder.normalizeReplyShape("{这不是 JSON}")).isEqualTo("{这不是 JSON}");
        assertThat(promptBuilder.normalizeReplyShape(null)).isNull();
    }

    @Test
    void shouldInjectDraftExcerptAsReadOnlyReference() {
        String systemPrompt = promptBuilder.buildSystemPrompt(
                AgentStage.CONFUSION, "这是一段比配置上限更长的草稿正文内容，用来验证截断行为是否生效");

        assertThat(systemPrompt).contains("只读参考，禁止改写或替换");
        assertThat(systemPrompt).contains("...");
    }

    @Test
    void shouldOmitDraftSectionWhenExcerptIsBlank() {
        assertThat(promptBuilder.buildSystemPrompt(AgentStage.EMOTION, "   "))
                .doesNotContain("只读参考");
    }

    @Test
    void shouldStartWithSystemMessageAndKeepHistoryWithinWindow() {
        List<AgentMessage> history = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            history.add(message(i, i % 2 == 1 ? AgentMessageRole.USER : AgentMessageRole.ASSISTANT, "第" + i + "条"));
        }

        List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                AgentStage.CORE_QUESTION, history, null);

        assertThat(messages.get(0).get("role")).isEqualTo("system");
        // system + 窗口内 4 条历史 + 本轮指令
        assertThat(messages).hasSize(6);
        assertThat(messages.get(1).get("content")).isEqualTo("第3条");
        assertThat(messages.get(messages.size() - 1).get("role")).isEqualTo("user");
    }

    @Test
    void shouldMapAssistantRoleForProviderPayload() {
        List<AgentMessage> history = List.of(
                message(1, AgentMessageRole.ASSISTANT, "今天是什么让你想写下这一刻？"),
                message(1, AgentMessageRole.USER, "工作上有点撑不住"));

        List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                AgentStage.CONFUSION, history, null);

        assertThat(messages.get(1).get("role")).isEqualTo("assistant");
        assertThat(messages.get(2).get("role")).isEqualTo("user");
    }

    @Test
    void shouldInstructClosingWithoutNewQuestion() {
        List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                AgentStage.CLOSING, List.of(), null);

        assertThat(messages.get(messages.size() - 1).get("content")).contains("收束");
        assertThat(messages.get(messages.size() - 1).get("content")).contains("不要再提新问题");
    }

    @Test
    void shouldBuildMaterialMessagesThatForbidAddedAnalysis() {
        List<Map<String, String>> messages = promptBuilder.buildMaterialMessages(
                List.of(message(1, AgentMessageRole.USER, "最近总是睡不好")));

        String system = messages.get(0).get("content");
        assertThat(system).contains("只使用用户说过的内容");
        assertThat(system).contains("\"material\"");
        assertThat(system).contains("不添加你的分析");
    }

    private AgentMessage message(int turnNo, AgentMessageRole role, String content) {
        AgentMessage message = new AgentMessage();
        message.setTurnNo(turnNo);
        message.setRole(role);
        message.setStage(AgentStage.EMOTION);
        message.setContent(content);
        return message;
    }
}
