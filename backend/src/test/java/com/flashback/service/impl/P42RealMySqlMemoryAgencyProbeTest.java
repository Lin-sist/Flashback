package com.flashback.service.impl;

import com.flashback.agent.memory.MemoryPort;
import com.flashback.agent.memory.MemoryQuery;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P4.2 Gate 3a：真实 MySQL 授权、候选隔离、来源外键、事务回滚与清理探针。 */
@EnabledIfEnvironmentVariable(named = "P42_MYSQL_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.ai.real-mode-mock-enabled=true",
        "logging.level.com.flashback.mapper=OFF",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-"
})
@ActiveProfiles("dev")
class P42RealMySqlMemoryAgencyProbeTest {
    private static final long OWNER_ID = 9_942_100L;
    private static final long OTHER_ID = 9_942_200L;
    private static final long CURRENT_RECORD_ID = 9_942_101L;
    private static final long ELIGIBLE_RECORD_ID = 9_942_102L;
    private static final long EXCLUDED_RECORD_ID = 9_942_103L;
    private static final long SEALED_RECORD_ID = 9_942_104L;
    private static final long BLOCKED_RECORD_ID = 9_942_105L;
    private static final long OTHER_RECORD_ID = 9_942_201L;
    private static final long SESSION_ID = 9_942_301L;
    private static final long RUNTIME_SESSION_ID = 9_942_302L;
    private static final long MESSAGE_ID = 9_942_401L;
    private static final long ROLLBACK_MESSAGE_ID = 9_942_402L;
    private static final long OPERATION_ID = 9_942_501L;
    private static final long OPERATION_ITEM_ID = 9_942_502L;

    @Autowired private AgentChatService service;
    @Autowired private MemoryPort memoryPort;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactionTemplate;

    @DynamicPropertySource
    static void realDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:mysql://127.0.0.1:3306/flashback"
                        + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    @Test
    void realMySqlMustEnforceMemoryAgencySchemaIsolationRollbackAndCleanup() {
        assertSchema();
        assertCleanStart();
        insertFixtures();
        try {
            assertAuthorizationAndOwnerScope();
            assertEligibilityMatrix();
            assertRuntimeOffOnAndRevocation();
            assertSourceResolutionAndDeleteSetNull();
            assertMessageAndSourceRollbackTogether();
            System.out.println("P42MYSQL PASS schema=true auth=true eligibility=true owner=true status=true "
                    + "exclusion=true deletion=true setNull=true rollback=true source=true");
        } finally {
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P42MYSQL CLEANUP users=0 records=0 sessions=0 messages=0 sources=0 operations=0");
    }

    private void assertSchema() {
        Integer policyColumns = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE()
                  AND ((table_name='agent_session' AND column_name='cross_record_memory_enabled')
                    OR (table_name='record' AND column_name IN
                      ('agent_memory_excluded','agent_memory_context_note')))
                """, Integer.class);
        Integer sourceColumns = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='agent_memory_source'
                """, Integer.class);
        String deleteRule = jdbc.queryForObject("""
                SELECT delete_rule FROM information_schema.referential_constraints
                WHERE constraint_schema=DATABASE()
                  AND table_name='agent_memory_source'
                  AND constraint_name='fk_agent_memory_source_record_id'
                """, String.class);
        assertThat(policyColumns).isEqualTo(3);
        assertThat(sourceColumns).isEqualTo(7);
        assertThat(deleteRule).isEqualTo("SET NULL");
    }

    private void assertCleanStart() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        assertThat(users).as("stale P4.2 MySQL probe state must be handled manually").isZero();
    }

    private void insertFixtures() {
        insertUser(OWNER_ID, "p42-mysql-owner");
        insertUser(OTHER_ID, "p42-mysql-other");
        insertRecord(CURRENT_RECORD_ID, OWNER_ID, "synthetic-current", "SAVED", false);
        insertRecord(ELIGIBLE_RECORD_ID, OWNER_ID, "方向仍然让我犹豫", "SAVED", false);
        insertRecord(EXCLUDED_RECORD_ID, OWNER_ID, "方向仍然让我犹豫", "SAVED", true);
        insertRecord(SEALED_RECORD_ID, OWNER_ID, "方向仍然让我犹豫", "SEALED", false);
        insertRecord(BLOCKED_RECORD_ID, OWNER_ID, "方向仍然让我犹豫", "SAVED", false);
        insertRecord(OTHER_RECORD_ID, OTHER_ID, "方向仍然让我犹豫", "SAVED", false);
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,conversation_intent,stage,status,"
                        + "turn_count,stage_reask_count,last_active_at,created_at,updated_at) "
                        + "VALUES (?,?,?,'WRITING_GUIDANCE','LISTEN','WITNESS','ACTIVE',0,0,NOW(),NOW(),NOW())",
                SESSION_ID, OWNER_ID, CURRENT_RECORD_ID);
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,conversation_intent,stage,status,"
                        + "turn_count,stage_reask_count,last_active_at,created_at,updated_at) "
                        + "VALUES (?,?,?,'WRITING_GUIDANCE','LISTEN','WITNESS','ACTIVE',0,0,NOW(),NOW(),NOW())",
                RUNTIME_SESSION_ID, OWNER_ID, CURRENT_RECORD_ID);
        jdbc.update("INSERT INTO agent_message(id,session_id,user_id,role,turn_no,stage,content,created_at) "
                        + "VALUES (?,?,?,'ASSISTANT',0,'WITNESS','synthetic',NOW())",
                MESSAGE_ID, SESSION_ID, OWNER_ID);
        jdbc.update("INSERT INTO data_operation(id,user_id,operation_type,status,total_items,processed_items,"
                        + "failed_items,created_at,updated_at) VALUES (?,?,'DELETE_RECORD','RUNNING',1,0,0,NOW(),NOW())",
                OPERATION_ID, OWNER_ID);
        jdbc.update("INSERT INTO data_operation_record(id,operation_id,user_id,record_id,item_status,attempt_count,"
                        + "created_at,updated_at) VALUES (?,?,?,?,'RUNNING',0,NOW(),NOW())",
                OPERATION_ITEM_ID, OPERATION_ID, OWNER_ID, BLOCKED_RECORD_ID);
    }

    private void assertAuthorizationAndOwnerScope() {
        AgentSessionVO initial = service.getSession(OWNER_ID, SESSION_ID);
        assertThat(initial.isCrossRecordMemoryEnabled()).isFalse();
        assertThat(service.switchMemoryAuthorization(OWNER_ID, SESSION_ID, true)
                .isCrossRecordMemoryEnabled()).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT cross_record_memory_enabled FROM agent_session WHERE id=?", Boolean.class, SESSION_ID))
                .isTrue();
        assertThat(service.switchMemoryAuthorization(OWNER_ID, SESSION_ID, false)
                .isCrossRecordMemoryEnabled()).isFalse();
        assertThatThrownBy(() -> service.getSession(OTHER_ID, SESSION_ID))
                .isInstanceOf(NotFoundException.class);
    }

    private void assertEligibilityMatrix() {
        List<Long> resultIds = memoryPort.retrieve(new MemoryQuery(
                        OWNER_ID,
                        AgentSessionPurpose.WRITING_GUIDANCE,
                        List.of("方向"),
                        List.of(),
                        CURRENT_RECORD_ID,
                        10)).stream()
                .map(fragment -> fragment.recordId())
                .toList();
        assertThat(resultIds).containsExactly(ELIGIBLE_RECORD_ID);
    }

    private void assertRuntimeOffOnAndRevocation() {
        AgentSessionVO authorizationOff = service.sendMessage(
                OWNER_ID, RUNTIME_SESSION_ID, messageRequest("最近又担心方向"));
        assertThat(authorizationOff.getMessages()).last().satisfies(message ->
                assertThat(message.getMemorySources()).isEmpty());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_memory_source WHERE session_id=?",
                Integer.class, RUNTIME_SESSION_ID)).isZero();

        service.switchMemoryAuthorization(OWNER_ID, RUNTIME_SESSION_ID, true);
        AgentSessionVO authorizationOn = service.sendMessage(
                OWNER_ID, RUNTIME_SESSION_ID, messageRequest("方向仍然让我犹豫"));
        assertThat(authorizationOn.getMessages()).last().satisfies(message ->
                assertThat(message.getMemorySources()).singleElement().satisfies(source -> {
                    assertThat(source.isAvailable()).isTrue();
                    assertThat(source.getRecordId()).isEqualTo(ELIGIBLE_RECORD_ID);
                }));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_memory_source WHERE session_id=?",
                Integer.class, RUNTIME_SESSION_ID)).isEqualTo(1);

        service.switchMemoryAuthorization(OWNER_ID, RUNTIME_SESSION_ID, false);
        AgentSessionVO revoked = service.sendMessage(
                OWNER_ID, RUNTIME_SESSION_ID, messageRequest("撤销后继续说方向"));
        assertThat(revoked.getMessages()).last().satisfies(message ->
                assertThat(message.getMemorySources()).isEmpty());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_memory_source WHERE session_id=?",
                Integer.class, RUNTIME_SESSION_ID)).isEqualTo(1);
    }

    private void assertSourceResolutionAndDeleteSetNull() {
        jdbc.update("INSERT INTO agent_memory_source(user_id,session_id,assistant_message_id,source_record_id,"
                        + "source_kind,created_at) VALUES (?,?,?,?,'CROSS_RECORD',NOW())",
                OWNER_ID, SESSION_ID, MESSAGE_ID, ELIGIBLE_RECORD_ID);
        AgentSessionVO beforeDelete = service.getSession(OWNER_ID, SESSION_ID);
        assertThat(beforeDelete.getMessages()).singleElement().satisfies(message ->
                assertThat(message.getMemorySources()).singleElement().satisfies(source -> {
                    assertThat(source.isAvailable()).isTrue();
                    assertThat(source.getRecordId()).isEqualTo(ELIGIBLE_RECORD_ID);
                }));

        jdbc.update("DELETE FROM `record` WHERE id=?", ELIGIBLE_RECORD_ID);
        assertThat(jdbc.queryForObject("SELECT source_record_id FROM agent_memory_source "
                + "WHERE assistant_message_id=?", Long.class, MESSAGE_ID)).isNull();
        AgentSessionVO afterDelete = service.getSession(OWNER_ID, SESSION_ID);
        assertThat(afterDelete.getMessages()).singleElement().satisfies(message ->
                assertThat(message.getMemorySources()).singleElement().satisfies(source -> {
                    assertThat(source.isAvailable()).isFalse();
                    assertThat(source.getRecordId()).isNull();
                    assertThat(source.getDisplayTitle()).isNull();
                    assertThat(source.getOccurredAt()).isNull();
                    assertThat(source.getContextNote()).isNull();
                }));
    }

    private void assertMessageAndSourceRollbackTogether() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO agent_message(id,session_id,user_id,role,turn_no,stage,content,created_at) "
                            + "VALUES (?,?,?,'ASSISTANT',1,'WITNESS','synthetic',NOW())",
                    ROLLBACK_MESSAGE_ID, SESSION_ID, OWNER_ID);
            jdbc.update("INSERT INTO agent_memory_source(user_id,session_id,assistant_message_id,source_record_id,"
                            + "source_kind,created_at) VALUES (?,?,?,?,'CROSS_RECORD',NOW())",
                    OWNER_ID, SESSION_ID, ROLLBACK_MESSAGE_ID, CURRENT_RECORD_ID);
            jdbc.update("INSERT INTO agent_memory_source(user_id,session_id,assistant_message_id,source_record_id,"
                            + "source_kind,created_at) VALUES (?,?,?,?,'CROSS_RECORD',NOW())",
                    OWNER_ID, SESSION_ID, ROLLBACK_MESSAGE_ID, CURRENT_RECORD_ID);
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_message WHERE id=?", Integer.class, ROLLBACK_MESSAGE_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_memory_source WHERE assistant_message_id=?",
                Integer.class, ROLLBACK_MESSAGE_ID)).isZero();
    }

    private void insertUser(long id, String username) {
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) "
                        + "VALUES (?,?,?,'P4.2 MySQL probe','ENABLED',NOW(),NOW())",
                id, username, "synthetic");
    }

    private void insertRecord(long id, long userId, String title, String status, boolean excluded) {
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,ai_summary,record_type,status,"
                        + "agent_memory_excluded,created_at,updated_at) "
                        + "VALUES (?,?,?,'synthetic',?,'MOMENT',?,?,NOW(),NOW())",
                id, userId, title, title, status, excluded);
    }

    private AgentMessageRequest messageRequest(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    private void cleanupSyntheticState() {
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", OWNER_ID, OTHER_ID);
    }

    private void assertSyntheticStateRemoved() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        Integer records = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id BETWEEN ? AND ?",
                Integer.class, CURRENT_RECORD_ID, OTHER_RECORD_ID);
        Integer sessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_session WHERE id IN (?,?)",
                Integer.class, SESSION_ID, RUNTIME_SESSION_ID);
        Integer messages = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_message WHERE id IN (?,?)",
                Integer.class, MESSAGE_ID, ROLLBACK_MESSAGE_ID);
        Integer sources = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_memory_source WHERE session_id=?", Integer.class, SESSION_ID);
        Integer operations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM data_operation WHERE id=?", Integer.class, OPERATION_ID);
        assertThat(List.of(users, records, sessions, messages, sources, operations)).containsOnly(0);
    }
}
