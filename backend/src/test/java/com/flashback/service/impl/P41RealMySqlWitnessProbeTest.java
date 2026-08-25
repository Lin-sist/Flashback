package com.flashback.service.impl;

import com.flashback.domain.AgentConversationIntent;
import com.flashback.common.exception.NotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P4.1 Gate 3c：真实 MySQL migration 后的合成 session 恢复与 intent 隔离探针。 */
@EnabledIfEnvironmentVariable(named = "P41_MYSQL_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-"
})
@ActiveProfiles("dev")
class P41RealMySqlWitnessProbeTest {
    private static final long OWNER_ID = 9_941_100L;
    private static final long OTHER_ID = 9_941_200L;
    private static final long RECORD_ID = 9_941_101L;
    private static final long WRITING_SESSION_ID = 9_941_301L;
    private static final long REVIEW_SESSION_ID = 9_941_302L;

    @Autowired private AgentChatService service;
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
    void realMySqlMustRecoverWitnessIntentWithoutLeakingAcrossPurposeOrOwner() {
        assertSchema();
        assertCleanStart();
        insertFixtures();
        try {
            AgentSessionVO recovered = service.getSession(OWNER_ID, WRITING_SESSION_ID);
            assertThat(recovered.getConversationIntent()).isEqualTo("LISTEN");
            assertThat(recovered.getStage()).isEqualTo("WITNESS");

            List<?> normalized = jdbc.queryForList(
                    "SELECT conversation_intent,stage,stage_reask_count FROM agent_session WHERE id=?",
                    WRITING_SESSION_ID);
            assertThat(normalized).hasSize(1);
            assertThat(jdbc.queryForObject(
                    "SELECT stage_reask_count FROM agent_session WHERE id=?", Integer.class, WRITING_SESSION_ID))
                    .isZero();

            AgentSessionVO switched = service.switchConversationIntent(
                    OWNER_ID, WRITING_SESSION_ID, AgentConversationIntent.UNTANGLE);
            assertThat(switched.getConversationIntent()).isEqualTo("UNTANGLE");
            assertThat(switched.getStage()).isEqualTo("WITNESS");
            assertThat(switched.getTurnCount()).isZero();

            AgentSessionVO review = service.getSession(OWNER_ID, REVIEW_SESSION_ID);
            assertThat(review.getConversationIntent()).isNull();
            assertThat(review.getStage()).isEqualTo("REVIEW");

            assertThatThrownBy(() -> service.getSession(OTHER_ID, WRITING_SESSION_ID))
                    .isInstanceOf(NotFoundException.class);

            System.out.println("P41MYSQL PASS schema=true recovery=true switch=true reviewNull=true ownerScope=true");
        } finally {
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P41MYSQL CLEANUP users=removed records=removed sessions=removed");
    }

    private void assertSchema() {
        Integer columns = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema=DATABASE() AND table_name='agent_session'
                  AND column_name IN ('purpose','conversation_intent','stage','stage_reask_count')
                """, Integer.class);
        assertThat(columns).isEqualTo(4);
    }

    private void assertCleanStart() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        assertThat(users).as("stale P4.1 MySQL probe state must be handled manually").isZero();
    }

    private void insertFixtures() {
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) "
                        + "VALUES (?,?,?,?,'ENABLED',NOW(),NOW())",
                OWNER_ID, "p41-mysql-owner", "synthetic", "P4.1 MySQL probe");
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) "
                        + "VALUES (?,?,?,?,'ENABLED',NOW(),NOW())",
                OTHER_ID, "p41-mysql-other", "synthetic", "P4.1 MySQL probe");
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,record_type,status,created_at,updated_at) "
                        + "VALUES (?,?,?,?,'MOMENT','SAVED',NOW(),NOW())",
                RECORD_ID, OWNER_ID, "synthetic", "synthetic");
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,conversation_intent,stage,status,"
                        + "turn_count,stage_reask_count,last_active_at,created_at,updated_at) "
                        + "VALUES (?,?,?,'WRITING_GUIDANCE',NULL,'EMOTION','ACTIVE',0,1,NOW(),NOW(),NOW())",
                WRITING_SESSION_ID, OWNER_ID, RECORD_ID);
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,conversation_intent,stage,status,"
                        + "turn_count,stage_reask_count,last_active_at,created_at,updated_at) "
                        + "VALUES (?,?,?,'REVIEW_CHAT',NULL,'REVIEW','ACTIVE',0,0,NOW(),NOW(),NOW())",
                REVIEW_SESSION_ID, OWNER_ID, RECORD_ID);
    }

    private void cleanupSyntheticState() {
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", OWNER_ID, OTHER_ID);
    }

    private void assertSyntheticStateRemoved() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID);
        Integer records = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id=?", Integer.class, RECORD_ID);
        Integer sessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent_session WHERE id IN (?,?)",
                Integer.class, WRITING_SESSION_ID, REVIEW_SESSION_ID);
        assertThat(List.of(users, records, sessions)).containsOnly(0);
    }
}
