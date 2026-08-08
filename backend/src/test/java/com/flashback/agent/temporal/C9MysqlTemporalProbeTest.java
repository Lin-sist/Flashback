package com.flashback.agent.temporal;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.memory.MemoryQuery;
import com.flashback.agent.memory.MySqlMemoryPort;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.mapper.RecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** C9 闸门 3：真实 MySQL owner/status/exclude/time/decay 合成验收。 */
@EnabledIfEnvironmentVariable(named = "C9_MYSQL_PROBE", matches = "1")
@SpringBootTest
@ActiveProfiles("dev")
class C9MysqlTemporalProbeTest {

    private static final Long USER_ID = 9_909_009L;
    private static final Long OTHER_USER_ID = 9_909_010L;
    private static final Long EXCLUDED_ID = 9_909_090L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @DynamicPropertySource
    static void localMysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:3306/flashback"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "123456"));
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("app.ai.provider", () -> "mock");
    }

    @Autowired private RecordMapper recordMapper;
    @Autowired private AppAgentProperties properties;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void realMysqlCandidatesMustRespectIsolationAndTemporalDecay() {
        cleanUp();
        try {
            insertFixtures();
            MySqlMemoryPort port = new MySqlMemoryPort(recordMapper, properties, CLOCK);
            List<MemoryFragment> retrieved = port.retrieve(new MemoryQuery(
                    USER_ID, AgentSessionPurpose.REVIEW_CHAT, List.of("C9时间"), List.of(),
                    EXCLUDED_ID, 3));
            assertThat(retrieved).extracting(MemoryFragment::recordId)
                    .containsExactly(9_909_003L, 9_909_002L, 9_909_001L);

            MemoryFragment focal = new MemoryFragment(EXCLUDED_ID,
                    LocalDateTime.of(2025, 1, 1, 10, 0), "2025年1月", "主记录".repeat(70));
            TemporalPolicyResult result = new AgentTemporalPolicy(properties, CLOCK).evaluate(
                    AgentChatMode.REVIEW_CHAT, "以前也有过类似的时候吗", List.of(focal), retrieved);
            assertThat(result.injectedFragments()).extracting(fragment -> fragment.text().length())
                    .containsExactly(210, 120, 90, 60);
            assertThat(result.patternEvidence().eligible()).isTrue();
            System.out.printf("C9MYSQL PASS retrieved=%d focalChars=%d recent=%d distant=%d longAgo=%d eligible=true%n",
                    retrieved.size(), result.injectedFragments().get(0).text().length(),
                    result.injectedFragments().get(1).text().length(), result.injectedFragments().get(2).text().length(),
                    result.injectedFragments().get(3).text().length());
        } finally {
            cleanUp();
        }
    }

    private void insertFixtures() {
        jdbcTemplate.update("INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at) VALUES (?, ?, 'x', ?, 'ACTIVE', NOW(), NOW())",
                USER_ID, "c9-mysql-probe", "c9-mysql-probe");
        jdbcTemplate.update("INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at) VALUES (?, ?, 'x', ?, 'ACTIVE', NOW(), NOW())",
                OTHER_USER_ID, "c9-mysql-other", "c9-mysql-other");
        insertRecord(9_909_001L, USER_ID, "UNLOCKED", "2025-01-01 10:00:00");
        insertRecord(9_909_002L, USER_ID, "UNLOCKED", "2026-04-01 10:00:00");
        insertRecord(9_909_003L, USER_ID, "UNLOCKED", "2026-07-20 10:00:00");
        insertRecord(EXCLUDED_ID, USER_ID, "UNLOCKED", "2026-07-25 10:00:00");
        insertRecord(9_909_004L, USER_ID, "SEALED", "2026-07-22 10:00:00");
        insertRecord(9_909_005L, OTHER_USER_ID, "UNLOCKED", "2026-07-23 10:00:00");
    }

    private void insertRecord(long id, long userId, String status, String createdAt) {
        jdbcTemplate.update("INSERT INTO `record` (id, user_id, title, content, status, record_type, ai_summary, created_at, updated_at) VALUES (?, ?, 'C9时间', 'synthetic', ?, 'NODE_RECORD', ?, ?, ?)",
                id, userId, status, "C9时间" + "甲".repeat(160), createdAt, createdAt);
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM `record` WHERE user_id IN (?, ?)", USER_ID, OTHER_USER_ID);
        jdbcTemplate.update("DELETE FROM `user` WHERE id IN (?, ?)", USER_ID, OTHER_USER_ID);
    }
}
