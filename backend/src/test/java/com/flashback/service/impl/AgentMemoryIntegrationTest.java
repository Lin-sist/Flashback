package com.flashback.service.impl;

import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.memory.MemoryPort;
import com.flashback.agent.memory.MemoryQuery;
import com.flashback.agent.tool.AgentToolRawArguments;
import com.flashback.agent.tool.AgentToolValidationResult;
import com.flashback.agent.tool.AgentToolValidator;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.service.AgentChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 记忆注入与来源分层的端到端测试（C3 agent-memory-retrieval）。
 *
 * 覆盖三条会被后续改动悄悄破坏的不变量：
 * 1. 会话用途落库为 WRITING_GUIDANCE，且不存在 REVIEW_CHAT 行为（本刀范围守护）；
 * 2. **正文只认会话层**——记忆内容被当成正文素材时必须被拒（不变量 2）；
 * 3. **来源只含本轮实际注入的片段**——检索到但未注入的不算来源（不变量 1）。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentMemoryIntegrationTest {

    private static final Long USER_ID = 8901L;
    private static final String MEMORY_TEXT = "那时候项目截止日期压得我喘不过气";
    private static final String NOT_INJECTED_TEXT = "去年冬天我在考虑要不要换城市生活";

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AgentPromptBuilder promptBuilder;

    @Autowired
    private AgentToolValidator toolValidator;

    @Autowired
    private AppAgentProperties properties;

    @Autowired
    private MemoryPort memoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertUser() {
        jdbcTemplate.update(
                "INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER_ID, "agent-memory-user", "test-hash", "Agent Memory User", "ENABLED");
    }

    private AgentMessage user(String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(AgentMessageRole.USER);
        message.setContent(content);
        return message;
    }

    private AgentLayeredCorpus corpusWithMemory(String sessionSaid) {
        List<AgentMessage> history = new ArrayList<>();
        history.add(user(sessionSaid));
        return AgentLayeredCorpus.of(
                history, List.of(MEMORY_TEXT), properties.getGuardrail().getFaithfulnessNgramSize());
    }

    // ---------- 会话用途 ----------

    @Test
    void shouldPersistPurposeAsWritingGuidance() {
        var opened = agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());

        String purpose = jdbcTemplate.queryForObject(
                "SELECT purpose FROM agent_session WHERE id = ?", String.class, opened.getSessionId());

        assertThat(purpose).isEqualTo("WRITING_GUIDANCE");
    }

    @Test
    void shouldNotCreateAnyReviewChatSession() {
        agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());
        agentChatService.sendMessage(
                USER_ID,
                jdbcTemplate.queryForObject(
                        "SELECT MAX(id) FROM agent_session WHERE user_id = ?", Long.class, USER_ID),
                request("最近工作上有点撑不住"));

        Integer reviewChatCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_session WHERE purpose = 'REVIEW_CHAT'", Integer.class);

        assertThat(reviewChatCount)
                .as("回看会话属后一刀 agent-review-chat，本刀不得产生任何该用途的会话")
                .isZero();
    }

    // ---------- 注入文本 ----------

    @Test
    void memorySupplementShouldCarryReadableTimeAnchorAndNotBeMaterial() {
        String supplement = promptBuilder.buildMemorySupplement(List.of(
                new MemoryFragment(42L, LocalDateTime.of(2026, 3, 14, 21, 0), "2026年3月", MEMORY_TEXT)));

        assertThat(supplement)
                .contains("2026年3月")
                .contains("记录42")
                .contains(MEMORY_TEXT);
        assertThat(supplement)
                .as("prompt 必须明说记忆不是正文素材，与后端硬拦形成双层")
                .contains("不是这次记录的正文素材");
        assertThat(supplement)
                .as("prompt 必须要求说清时间，它是时间归属护栏的语义目标")
                .contains("说清那是过去哪个时候的事");
    }

    @Test
    void memorySupplementShouldBeEmptyWhenNothingRetrieved() {
        assertThat(promptBuilder.buildMemorySupplement(List.of())).isEmpty();
        assertThat(promptBuilder.buildMemorySupplement(null)).isEmpty();
    }

    // ---------- 不变量 2：正文只认会话层 ----------

    @Test
    void mustRejectMemoryTextAsRecordContent() {
        AgentLayeredCorpus corpus = corpusWithMemory("今天又被临时加了需求");

        AgentToolValidationResult result = toolValidator.validate(
                "append_record_content",
                new AgentToolRawArguments("要不要把这些放进正文？", MEMORY_TEXT, null, null),
                true,
                corpus);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason())
                .as("把三个月前写的句子搬进今天的记录，要与「编了一句话」区分留痕")
                .isEqualTo(AgentToolValidationResult.REASON_MEMORY_AS_CONTENT);
    }

    @Test
    void shouldStillAcceptSessionSourcedContentWhenMemoryPresent() {
        String sessionSaid = "今天又被临时加了需求，心里挺烦的";
        AgentLayeredCorpus corpus = corpusWithMemory(sessionSaid);

        AgentToolValidationResult result = toolValidator.validate(
                "append_record_content",
                new AgentToolRawArguments("要不要把这些放进正文？", sessionSaid, null, null),
                true,
                corpus);

        assertThat(result.isAccepted())
                .as("引入记忆层不得削弱正常路径：本次说过的话仍应正常通过")
                .isTrue();
    }

    @Test
    void shouldReportPlainUnfaithfulWhenTextMatchesNeitherLayer() {
        AgentLayeredCorpus corpus = corpusWithMemory("今天又被临时加了需求");

        AgentToolValidationResult result = toolValidator.validate(
                "append_record_content",
                new AgentToolRawArguments("要不要放进正文？",
                        "他其实一直在犹豫要不要辞职去别的城市重新开始生活，只是没说出口", null, null),
                true,
                corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
    }

    // ---------- 不变量 1：只含本轮实际注入的片段 ----------

    @Test
    void notInjectedHistoryMustNotBecomeLegalSource() {
        AgentLayeredCorpus corpus = corpusWithMemory("今天又被临时加了需求");

        AgentToolValidationResult result = toolValidator.validate(
                "append_record_content",
                new AgentToolRawArguments("要不要放进正文？", NOT_INJECTED_TEXT, null, null),
                true,
                corpus);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason())
                .as("未注入的历史内容既不是会话来源也不是记忆来源，应判普通不忠实")
                .isEqualTo(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
    }

    // ---------- 无记忆层时等价于 C4 现状 ----------

    @Test
    void sessionOnlyCorpusMustBehaveExactlyAsBeforeMemory() {
        AgentSourceCorpus session = AgentSourceCorpus.ofTexts(
                List.of("今天又被临时加了需求"), properties.getGuardrail().getFaithfulnessNgramSize());

        AgentToolValidationResult viaLegacySignature = toolValidator.validate(
                "append_record_content",
                new AgentToolRawArguments("要不要放进正文？", MEMORY_TEXT, null, null),
                true,
                session);

        assertThat(viaLegacySignature.rejectReason())
                .as("无记忆层时，记忆文本只是一段无来源文本，判普通不忠实")
                .isEqualTo(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
    }

    // ---------- 检索无线索不查库 ----------

    @Test
    void retrievalWithoutCueShouldReturnEmpty() {
        List<MemoryFragment> fragments = memoryPort.retrieve(new MemoryQuery(
                USER_ID, com.flashback.domain.AgentSessionPurpose.WRITING_GUIDANCE,
                List.of(), List.of(), null, 3));

        assertThat(fragments).isEmpty();
    }

    // ---------- 记忆开关 ----------

    @Test
    void conversationShouldWorkWithMemoryDisabled() {
        properties.getMemory().setEnabled(false);
        try {
            var opened = agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());
            var afterTurn = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), request("最近工作上有点撑不住"));

            assertThat(afterTurn.getStatus()).isEqualTo("SUCCESS");
        } finally {
            properties.getMemory().setEnabled(true);
        }
    }

    private AgentMessageRequest request(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }
}
