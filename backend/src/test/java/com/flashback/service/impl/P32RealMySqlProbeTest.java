package com.flashback.service.impl;

import com.flashback.domain.DataDeletionScope;
import com.flashback.domain.DataOperationStatus;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.DataOwnershipService;
import com.flashback.service.data.DataOwnershipMutationGuard;
import com.flashback.vo.DataOperationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P3.2 Gate 3a：真实 MySQL migration 后的 owner scope、cascade 与中断恢复探针。 */
@EnabledIfEnvironmentVariable(named = "P32_MYSQL_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-",
        "app.data-ownership.recovery-delay-ms=3600000",
        "app.data-ownership.cleanup-delay-ms=3600000",
        "logging.level.com.flashback=INFO"
})
@ActiveProfiles("dev")
class P32RealMySqlProbeTest {
    private static final long OWNER_ID = 9_932_100L;
    private static final long OTHER_ID = 9_932_200L;
    private static final String OWNER_NAME = "p32-mysql-owner";
    private static final String OTHER_NAME = "p32-mysql-other";
    private static final long SINGLE_RECORD_ID = 9_932_101L;
    private static final long DRAFT_ID = 9_932_111L;
    private static final long SAVED_ID = 9_932_112L;
    private static final long SEALED_ID = 9_932_113L;
    private static final long UNLOCKED_ID = 9_932_114L;
    private static final long LATE_RECORD_ID = 9_932_115L;
    private static final long OTHER_RECORD_ID = 9_932_201L;
    private static final long TAG_ID = 9_932_301L;
    private static final long SESSION_ID = 9_932_401L;

    @Autowired private DataOwnershipService service;
    @Autowired private DataOwnershipServiceImpl serviceImpl;
    @Autowired private DataOwnershipMutationGuard mutationGuard;
    @Autowired private RecordMapper recordMapper;
    @Autowired private JdbcTemplate jdbc;

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
    void realMySqlMustPreserveOwnerScopeCascadeAndPersistedRecovery() {
        assertP32Schema();
        assertCleanStart();
        insertUser(OWNER_ID, OWNER_NAME);
        insertUser(OTHER_ID, OTHER_NAME);
        try {
            probeSingleRecordOwnerScopeAndIdempotency();
            probeClearAllFixedSnapshotCascadeAndRecovery();
            System.out.println("P32MYSQL PASS migration=true single=true ownerScope=true clearAll=true "
                    + "fixedSnapshot=true mutationFreeze=true cascade=true recovery=true");
        } finally {
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P32MYSQL CLEANUP users=removed records=removed operations=removed");
    }

    private void probeSingleRecordOwnerScopeAndIdempotency() {
        insertRecord(SINGLE_RECORD_ID, OWNER_ID, "SAVED");
        assertThatThrownBy(() -> service.prepareDeletion(OTHER_ID, DataDeletionScope.RECORD, SINGLE_RECORD_ID))
                .isInstanceOf(RuntimeException.class);

        DataOperationVO intent = service.prepareDeletion(OWNER_ID, DataDeletionScope.RECORD, SINGLE_RECORD_ID);
        assertThat(intent.getStatus()).isEqualTo(DataOperationStatus.PREPARED);
        assertThat(intent.getConfirmationText()).isNotBlank();
        String persistedHash = jdbc.queryForObject(
                "SELECT confirmation_nonce_hash FROM data_operation WHERE id=?", String.class, intent.getId());
        assertThat(persistedHash).isNotEqualTo(intent.getConfirmationText()).hasSize(64);

        DataOperationVO completed = service.confirmDeletion(OWNER_ID, intent.getId(), intent.getConfirmationText());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recordCount(SINGLE_RECORD_ID, OWNER_ID)).isZero();
        assertThat(service.confirmDeletion(OWNER_ID, intent.getId(), intent.getConfirmationText()).getId())
                .isEqualTo(intent.getId());
    }

    private void probeClearAllFixedSnapshotCascadeAndRecovery() {
        insertRecord(DRAFT_ID, OWNER_ID, "DRAFT");
        insertRecord(SAVED_ID, OWNER_ID, "SAVED");
        insertRecord(SEALED_ID, OWNER_ID, "SEALED");
        insertRecord(UNLOCKED_ID, OWNER_ID, "UNLOCKED");
        insertRecord(OTHER_RECORD_ID, OTHER_ID, "SAVED");
        insertDerivedRows();

        DataOperationVO intent = service.prepareDeletion(OWNER_ID, DataDeletionScope.ALL_RECORDS, null);
        assertThat(intent.getTotalItems()).isEqualTo(4);
        insertRecord(LATE_RECORD_ID, OWNER_ID, "SAVED");
        jdbc.update("UPDATE data_operation SET status='RUNNING', confirmed_at=NOW(), "
                + "started_at=NOW(), updated_at=DATE_SUB(NOW(), INTERVAL 30 MINUTE) WHERE id=? AND status='PREPARED'",
                intent.getId());

        assertThatThrownBy(() -> mutationGuard.assertWritable(OWNER_ID)).isInstanceOf(RuntimeException.class);
        assertThat(recordMapper.selectByIdAndUserId(SAVED_ID, OWNER_ID)).isNull();
        assertThat(recordMapper.selectByIdAndUserIdForDeletion(SAVED_ID, OWNER_ID)).isNotNull();

        serviceImpl.resumeStaleOperations();
        DataOperationVO recovered = service.getOperation(OWNER_ID, intent.getId());
        assertThat(recovered.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recovered.getProcessedItems()).isEqualTo(4);
        assertThat(recovered.getFailedItems()).isZero();
        assertThat(recordCount(DRAFT_ID, OWNER_ID)).isZero();
        assertThat(recordCount(SAVED_ID, OWNER_ID)).isZero();
        assertThat(recordCount(SEALED_ID, OWNER_ID)).isZero();
        assertThat(recordCount(UNLOCKED_ID, OWNER_ID)).isZero();
        assertThat(recordCount(LATE_RECORD_ID, OWNER_ID)).isOne();
        assertThat(recordCount(OTHER_RECORD_ID, OTHER_ID)).isOne();
        assertThat(count("record_location", OWNER_ID)).isZero();
        assertThat(count("reply", OWNER_ID)).isZero();
        assertThat(count("record_reminder", OWNER_ID)).isZero();
        assertThat(count("unlock_notice_log", OWNER_ID)).isZero();
        assertThat(count("agent_session", OWNER_ID)).isZero();
        assertThat(count("agent_message", OWNER_ID)).isZero();
        assertThat(count("agent_tool_call", OWNER_ID)).isZero();
        assertThat(count("agent_turn_trace", OWNER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_tag WHERE record_id IN (?,?,?,?)",
                Integer.class, DRAFT_ID, SAVED_ID, SEALED_ID, UNLOCKED_ID)).isZero();
        mutationGuard.assertWritable(OWNER_ID);
    }

    private void assertP32Schema() {
        Integer tables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema=DATABASE() AND table_name IN ('data_operation','data_operation_record')
                """, Integer.class);
        Integer foreignKeys = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.referential_constraints
                WHERE constraint_schema=DATABASE()
                  AND table_name IN ('data_operation','data_operation_record')
                """, Integer.class);
        Integer indexes = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
                WHERE table_schema=DATABASE() AND table_name IN ('data_operation','data_operation_record')
                """, Integer.class);
        assertThat(tables).isEqualTo(2);
        assertThat(foreignKeys).isEqualTo(4);
        assertThat(indexes).isGreaterThanOrEqualTo(7);
    }

    private void insertDerivedRows() {
        LocalDateTime now = now();
        jdbc.update("INSERT INTO record_location(record_id,user_id,source,name,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                SAVED_ID, OWNER_ID, "MANUAL", "synthetic", now, now);
        jdbc.update("INSERT INTO reply(record_id,user_id,content,reply_type,created_at) VALUES (?,?,?,?,?)",
                UNLOCKED_ID, OWNER_ID, "synthetic", "SHORT_REPLY", now);
        jdbc.update("INSERT INTO tag(id,name,type,status,created_at) VALUES (?,?,?,?,?)",
                TAG_ID, "p32-mysql-tag", "TOPIC", "ENABLED", now);
        jdbc.update("INSERT INTO record_tag(record_id,tag_id) VALUES (?,?)", SAVED_ID, TAG_ID);
        jdbc.update("INSERT INTO record_reminder(record_id,user_id,template_type,reminder_status,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                SEALED_ID, OWNER_ID, "UNLOCK_REMINDER", "AUTHORIZED", now, now);
        jdbc.update("INSERT INTO unlock_notice_log(record_id,user_id,notice_type,notice_status,created_at) VALUES (?,?,?,?,?)",
                SEALED_ID, OWNER_ID, "SYSTEM_UNLOCK", "SUCCESS", now);
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,stage,status,turn_count,stage_reask_count,last_active_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                SESSION_ID, OWNER_ID, SEALED_ID, "WRITING_GUIDANCE", "OPENING", "ACTIVE", 0, 0, now, now, now);
        jdbc.update("INSERT INTO agent_message(session_id,user_id,role,turn_no,stage,content,created_at) VALUES (?,?,?,?,?,?,?)",
                SESSION_ID, OWNER_ID, "USER", 1, "OPENING", "synthetic", now);
        jdbc.update("INSERT INTO agent_tool_call(session_id,user_id,record_id,turn_no,tool_name,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",
                SESSION_ID, OWNER_ID, SEALED_ID, 1, "append_record_content", "PROPOSED", now, now);
        jdbc.update("INSERT INTO agent_turn_trace(trace_id,session_id,user_id,record_id,turn_no,attempt_no,purpose,stage,outcome,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                "fedcba9876543210fedcba9876543210", SESSION_ID, OWNER_ID, SEALED_ID,
                1, 1, "WRITING_GUIDANCE", "OPENING", "SUCCESS", now);
    }

    private void assertCleanStart() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?) OR username IN (?,?)",
                Integer.class, OWNER_ID, OTHER_ID, OWNER_NAME, OTHER_NAME);
        assertThat(users).as("stale P3.2 MySQL probe state must be handled manually").isZero();
    }

    private void insertUser(long id, String username) {
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) "
                        + "VALUES (?,?,?,?,?,?,?)",
                id, username, "synthetic", "P3.2 MySQL probe", "ENABLED", now(), now());
    }

    private void insertRecord(long id, long userId, String status) {
        LocalDateTime expiry = "DRAFT".equals(status) ? now().plusDays(7) : null;
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,record_type,status,draft_expires_at,created_at,updated_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                id, userId, "synthetic", "synthetic", "MOMENT", status, expiry, now(), now());
    }

    private int recordCount(long recordId, long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id=? AND user_id=?", Integer.class, recordId, userId);
    }

    private int count(String table, long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE user_id=?", Integer.class, userId);
    }

    private void cleanupSyntheticState() {
        jdbc.update("DELETE FROM data_operation WHERE user_id IN (?,?)", OWNER_ID, OTHER_ID);
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", OWNER_ID, OTHER_ID);
        jdbc.update("DELETE FROM tag WHERE id=?", TAG_ID);
    }

    private void assertSyntheticStateRemoved() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?) OR username IN (?,?)",
                Integer.class, OWNER_ID, OTHER_ID, OWNER_NAME, OTHER_NAME);
        Integer records = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        Integer operations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM data_operation WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        assertThat(List.of(users, records, operations)).containsOnly(0);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
