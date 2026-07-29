package com.flashback.agent.memory;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 检索线索提取测试（C3 agent-memory-retrieval）。
 *
 * 固定两条容易在后续改动中丢掉的语义：
 * 1. 只取用户自己的表达 —— 用 Agent 的提问去检索会命中 Agent 的语言习惯；
 * 2. 最新一句话优先占用关键词额度 —— 用户此刻在说的事才是检索方向。
 */
class MemoryCueExtractorTest {

    private AppAgentProperties properties;
    private MemoryCueExtractor extractor;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        extractor = new MemoryCueExtractor(properties);
    }

    private AgentMessage message(AgentMessageRole role, String content) {
        AgentMessage msg = new AgentMessage();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    @Test
    void shouldExtractKeywordsFromUserMessages() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "工作压力太大了，项目排期又变了"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords).contains("工作压力太大了", "项目排期又变了");
    }

    @Test
    void shouldIgnoreAssistantMessages() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.ASSISTANT, "今天是什么让你想写下这一刻"),
                message(AgentMessageRole.USER, "加班"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords)
                .as("用 Agent 的提问去检索会命中它自己的语言习惯，而不是用户的心事")
                .noneMatch(keyword -> keyword.contains("写下这一刻"));
    }

    @Test
    void shouldPrioritiseLatestUserMessage() {
        properties.getMemory().setMaxKeywords(1);
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "很早说的事情"),
                message(AgentMessageRole.USER, "刚刚说的事情"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords).containsExactly("刚刚说的事情");
    }

    @Test
    void shouldDropStopFragments() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "我 觉得 然后 加班"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords).doesNotContain("我", "觉得", "然后");
    }

    @Test
    void shouldDropFragmentsShorterThanMinLength() {
        properties.getMemory().setMinKeywordLength(4);
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "加班，项目排期变动"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords).allMatch(keyword -> keyword.length() >= 4);
    }

    @Test
    void shouldRespectMaxKeywords() {
        properties.getMemory().setMaxKeywords(2);
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "第一件事，第二件事，第三件事，第四件事"));

        assertThat(extractor.extractKeywords(history)).hasSize(2);
    }

    @Test
    void shouldRespectCueMessageWindow() {
        properties.getMemory().setCueMessageWindow(1);
        properties.getMemory().setMaxKeywords(10);
        List<AgentMessage> history = new ArrayList<>();
        history.add(message(AgentMessageRole.USER, "很久以前的话题"));
        history.add(message(AgentMessageRole.USER, "最近的话题"));

        List<String> keywords = extractor.extractKeywords(history);

        assertThat(keywords).containsExactly("最近的话题");
    }

    @Test
    void shouldReturnEmptyForNoUsableHistory() {
        assertThat(extractor.extractKeywords(null)).isEmpty();
        assertThat(extractor.extractKeywords(List.of())).isEmpty();
        assertThat(extractor.extractKeywords(List.of(
                message(AgentMessageRole.ASSISTANT, "只有我在说话")))).isEmpty();
        assertThat(extractor.extractKeywords(List.of(
                message(AgentMessageRole.USER, "   ")))).isEmpty();
    }

    @Test
    void shouldDeduplicateRepeatedFragments() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "工作压力，工作压力，工作压力"));

        assertThat(extractor.extractKeywords(history)).containsExactly("工作压力");
    }
}
