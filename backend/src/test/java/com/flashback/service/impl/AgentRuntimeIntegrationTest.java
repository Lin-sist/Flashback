package com.flashback.service.impl;

import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 H2 schema + MyBatis mapper + mock provider 串联多轮 Runtime。
 *
 * 不 mock AgentChatService，覆盖 session/message 落库、状态推进与中断恢复。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentRuntimeIntegrationTest {

    private static final Long USER_ID = 8801L;

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertUser() {
        jdbcTemplate.update(
                "INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER_ID,
                "agent-runtime-user",
                "test-hash",
                "Agent Runtime User",
                "ENABLED");
    }

    @Test
    void shouldPersistAdvanceAndResumeMultiTurnConversationWithMockProvider() {
        var opened = agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());

        assertThat(opened.getStatus()).isEqualTo("SUCCESS");
        assertThat(opened.getSource()).isEqualTo("mock");
        assertThat(opened.getStage()).isEqualTo(AgentStage.EMOTION.name());
        assertThat(opened.getMessages()).singleElement()
                .satisfies(message -> {
                    assertThat(message.getRole()).isEqualTo(AgentMessageRole.ASSISTANT.name());
                    assertThat(message.getTurnNo()).isZero();
                });

        var afterFirstTurn = agentChatService.sendMessage(
                USER_ID,
                opened.getSessionId(),
                message("工作上有点撑不住，最近一直睡不好"));

        assertThat(afterFirstTurn.getStatus()).isEqualTo("SUCCESS");
        assertThat(afterFirstTurn.getStage()).isEqualTo(AgentStage.CONFUSION.name());
        assertThat(afterFirstTurn.getTurnCount()).isEqualTo(1);
        assertThat(afterFirstTurn.getMessages()).extracting("role")
                .containsExactly("ASSISTANT", "USER", "ASSISTANT");

        var resumed = agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());

        assertThat(resumed.getSessionId()).isEqualTo(opened.getSessionId());
        assertThat(resumed.getStage()).isEqualTo(AgentStage.CONFUSION.name());
        assertThat(resumed.getMessages()).hasSize(3);

        var finished = agentChatService.finish(USER_ID, opened.getSessionId());

        assertThat(finished.getSessionStatus()).isEqualTo(AgentSessionStatus.ENDED.name());
        assertThat(finished.getStage()).isEqualTo(AgentStage.ENDED.name());
        assertThat(finished.getMaterialDraft()).isEqualTo("工作上有点撑不住，最近一直睡不好");
        assertThat(finished.isCanContinue()).isFalse();

        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_session WHERE id = ? AND user_id = ? AND status = 'ENDED'",
                Integer.class,
                opened.getSessionId(),
                USER_ID);
        Integer messageCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM agent_message WHERE session_id = ?",
                Integer.class,
                opened.getSessionId());
        assertThat(sessionCount).isEqualTo(1);
        assertThat(messageCount).isEqualTo(3);
    }

    @Test
    void shouldReaskOnceThenAdvanceForConsecutiveShortAnswers() {
        var opened = agentChatService.startOrResume(USER_ID, new AgentSessionStartRequest());

        var reasked = agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("嗯"));
        assertThat(reasked.getStage()).isEqualTo(AgentStage.EMOTION.name());

        var advanced = agentChatService.sendMessage(USER_ID, opened.getSessionId(), message("不知道"));
        assertThat(advanced.getStage()).isEqualTo(AgentStage.CONFUSION.name());
        assertThat(advanced.getTurnCount()).isEqualTo(2);
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }
}
