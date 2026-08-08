package com.flashback.agent.resilience;

import com.flashback.config.AppAiProperties;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentTurnTrace;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentTurnTraceMapper;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** C8：真实 MySQL 下验证 pending-turn retry 与分类 trace 持久化。 */
@EnabledIfEnvironmentVariable(named = "C8_MYSQL_PROBE", matches = "1")
@SpringBootTest
@ActiveProfiles("dev")
class C8MysqlResilienceProbeTest {

    private static final Long USER_ID = 9_908_008L;
    private static final Long RECORD_ID = 9_908_008L;

    @DynamicPropertySource
    static void localMysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:3306/flashback"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "123456"));
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("app.ai.provider", () -> "mock");
        registry.add("app.ai.real-mode-mock-enabled", () -> "false");
    }

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private AgentMessageMapper agentMessageMapper;

    @Autowired
    private AgentTurnTraceMapper traceMapper;

    @Autowired
    private AppAiProperties aiProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unavailableTurnShouldRetryOnSameTurnAndPersistTypedTraces() {
        cleanUp();
        try {
            insertFixture();
            AgentSessionStartRequest start = new AgentSessionStartRequest();
            start.setRecordId(RECORD_ID);
            AgentSessionVO opened = agentChatService.startOrResume(USER_ID, start);
            assertThat(opened.getStatus()).isEqualTo("UNAVAILABLE");

            AgentSessionVO unavailable = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message("这是 C8 MySQL 验收合成输入"));
            assertThat(unavailable.getStatus()).isEqualTo("UNAVAILABLE");

            aiProperties.setRealModeMockEnabled(true);
            AgentSessionVO retried = agentChatService.sendMessage(
                    USER_ID, opened.getSessionId(), message("这是 C8 MySQL 验收合成输入"));
            assertThat(retried.getStatus()).isEqualTo("SUCCESS");
            assertThat(retried.getTurnCount()).isEqualTo(1);

            assertThat(agentMessageMapper.selectBySessionId(opened.getSessionId()))
                    .filteredOn(message -> message.getRole() == AgentMessageRole.USER)
                    .hasSize(1);

            List<AgentTurnTrace> traces = traceMapper.selectBySessionId(opened.getSessionId());
            assertThat(traces).hasSize(2);
            assertThat(traces).extracting(AgentTurnTrace::getTurnNo).containsOnly(1);
            assertThat(traces).extracting(AgentTurnTrace::getAttemptNo).containsExactly(1, 2);
            assertThat(traces.get(0).getOutcome()).isEqualTo("UNAVAILABLE");
            assertThat(traces.get(0).getCauseType()).isEqualTo("auth-configuration");
            assertThat(traces.get(0).getStepsJson())
                    .contains("\"phase\":\"initial\"")
                    .contains("\"budgetExhausted\":false");
            assertThat(traces.get(1).getOutcome()).isEqualTo("SUCCESS");

            System.out.printf(
                    "C8MYSQL PASS session=%d turn=1 attempts=2 firstOutcome=%s firstCause=%s secondOutcome=%s%n",
                    opened.getSessionId(), traces.get(0).getOutcome(), traces.get(0).getCauseType(),
                    traces.get(1).getOutcome());
        } finally {
            aiProperties.setRealModeMockEnabled(false);
            cleanUp();
        }
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    private void insertFixture() {
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, 'c8-mysql-probe', 'x', 'c8-mysql-probe', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, USER_ID);
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, title, content, status, record_type, created_at, updated_at)
                VALUES (?, ?, 'C8 probe', 'synthetic', 'DRAFT', 'NODE_RECORD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, RECORD_ID, USER_ID);
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM agent_turn_trace WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_tool_call WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_message WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM agent_session WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM `record` WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", USER_ID);
    }
}
