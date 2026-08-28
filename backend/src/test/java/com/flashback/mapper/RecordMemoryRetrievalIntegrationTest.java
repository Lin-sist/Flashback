package com.flashback.mapper;

import com.flashback.domain.Record;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 记忆检索 SQL 的集成测试（C3 agent-memory-retrieval）。
 *
 * 用真实 SQL 而非 mock，因为本刀最严重的两个失败模式都只能在 SQL 层证伪：
 * 1. **跨用户泄露** —— 一旦 user_id 谓词被误改成可选，mock 测试完全看不出来；
 * 2. **匹配到正文** —— 只有真实执行才能证明 content 里的词不会命中。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordMemoryRetrievalIntegrationTest {

    private static final Long USER_A = 9101L;
    private static final Long USER_B = 9102L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 29, 10, 0);
    private static final LocalDateTime LOOKBACK_FROM = NOW.minusMonths(24);

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- 用户隔离（严重缺陷等级） ----------

    @Test
    void mustNotReturnOtherUsersRecordsEvenWhenHighlyRelevant() {
        insertUser(USER_A, "user-a");
        insertUser(USER_B, "user-b");
        // 用户 B 的记录与关键词高度相关——若隔离失效，这条一定会被命中。
        insertRecord(2001L, USER_B, "DRAFT", "项目排期", "项目排期压力", null, null, NOW.minusMonths(2));
        insertRecord(2002L, USER_A, "DRAFT", "无关标题", null, null, null, NOW.minusMonths(2));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).isEmpty();
    }

    // ---------- 不匹配正文 ----------

    @Test
    void mustNotMatchAgainstRecordContent() {
        insertUser(USER_A, "user-a");
        // 关键词只出现在 content 里，说明性字段完全不含它。
        insertRecord(2101L, USER_A, "DRAFT", "普通标题", null, null,
                "这段正文里写了独有关键词甲乙丙", NOW.minusMonths(1));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("独有关键词甲乙丙"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits)
                .as("正文是最高敏字段且无索引，检索谓词不得覆盖它")
                .isEmpty();
    }

    // ---------- 封存未解锁排除 ----------

    @Test
    void mustExcludeSealedRecords() {
        insertUser(USER_A, "user-a");
        insertRecord(2201L, USER_A, "SEALED", "项目排期焦虑", null, null, null, NOW.minusMonths(3));
        insertRecord(2202L, USER_A, "UNLOCKED", "项目排期焦虑", null, null, null, NOW.minusMonths(4));
        insertRecord(2203L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(5));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).extracting(Record::getId)
                .as("封存未解锁的内容用户自己都还看不到，Agent 不得提前复述")
                .containsExactlyInAnyOrder(2202L, 2203L);
    }

    // ---------- 字段范围 ----------

    @Test
    void shouldMatchAcrossDescriptiveFields() {
        insertUser(USER_A, "user-a");
        insertRecord(2301L, USER_A, "DRAFT", "关键甲", null, null, null, NOW.minusMonths(1));
        insertRecord(2302L, USER_A, "DRAFT", null, "关键甲", null, null, NOW.minusMonths(2));
        insertRecordWithSummary(2303L, USER_A, "关键甲", NOW.minusMonths(3));
        insertRecordWithBelief(2304L, USER_A, "关键甲", NOW.minusMonths(4));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("关键甲"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).extracting(Record::getId)
                .containsExactlyInAnyOrder(2301L, 2302L, 2303L, 2304L);
    }

    // ---------- 标签线索 ----------

    @Test
    void shouldMatchBySharedEnabledTag() {
        insertUser(USER_A, "user-a");
        insertTag(3001L, "工作焦虑", "ENABLED");
        insertTag(3002L, "已停用", "DISABLED");
        insertRecord(2401L, USER_A, "DRAFT", "毫不相关", null, null, null, NOW.minusMonths(1));
        insertRecord(2402L, USER_A, "DRAFT", "毫不相关", null, null, null, NOW.minusMonths(2));
        bindTag(2401L, 3001L);
        bindTag(2402L, 3002L);

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of(), List.of(3001L, 3002L), null, LOOKBACK_FROM, 10);

        assertThat(hits).extracting(Record::getId)
                .as("停用标签不应带来命中")
                .containsExactly(2401L);
    }

    // ---------- 排除与窗口 ----------

    @Test
    void shouldExcludeGivenRecordId() {
        insertUser(USER_A, "user-a");
        insertRecord(2501L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(1));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), 2501L, LOOKBACK_FROM, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    void shouldApplyLookbackWindow() {
        insertUser(USER_A, "user-a");
        insertRecord(2601L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(1));
        insertRecord(2602L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(40));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).extracting(Record::getId).containsExactly(2601L);
    }

    @Test
    void mustNotReturnExcludedRecords() {
        insertUser(USER_A, "user-a");
        insertRecord(2701L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(1));
        insertRecord(2702L, USER_A, "DRAFT", "项目排期压力", null, null, null, NOW.minusMonths(2));
        jdbcTemplate.update("UPDATE `record` SET agent_memory_excluded = 1 WHERE id = ?", 2701L);

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).extracting(Record::getId).containsExactly(2702L);
    }

    @Test
    void userContextNoteMustNotBecomeRetrievalCue() {
        insertUser(USER_A, "user-a");
        insertRecord(2751L, USER_A, "DRAFT", "无关标题", null, null, "无关正文", NOW.minusMonths(1));
        jdbcTemplate.update(
                "UPDATE `record` SET agent_memory_context_note = ? WHERE id = ?",
                "项目排期只代表当时",
                2751L);

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits)
                .as("用户后来说明只随实际来源进入 prompt，不参与 cue 或排序")
                .isEmpty();
    }

    /**
     * 无线索时恒不命中，而不是退化成「按时间倒序取最近几条」。
     * 后者不是记忆关联，是随机翻旧账。
     */
    @Test
    void shouldReturnNothingWhenNoCueGiven() {
        insertUser(USER_A, "user-a");
        insertRecord(2701L, USER_A, "DRAFT", "任何标题", null, null, null, NOW.minusMonths(1));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of(), List.of(), null, LOOKBACK_FROM, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    void shouldRespectLimit() {
        insertUser(USER_A, "user-a");
        insertRecord(2801L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(1));
        insertRecord(2802L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(2));
        insertRecord(2803L, USER_A, "DRAFT", "项目排期焦虑", null, null, null, NOW.minusMonths(3));

        List<Record> hits = recordMapper.selectMemoryCandidates(
                USER_A, List.of("项目排期"), List.of(), null, LOOKBACK_FROM, 2);

        // 时间倒序取前 2 条。
        assertThat(hits).extracting(Record::getId).containsExactly(2801L, 2802L);
    }

    // ---------- 数据准备 ----------

    private void insertUser(Long id, String username) {
        // user 与 record 在 H2 中是保留字，沿用既有集成测试的反引号写法。
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, ?, 'x', ?, 'ACTIVE', ?, ?)
                """, id, username, username, NOW, NOW);
    }

    private void insertRecord(
            Long id, Long userId, String status, String title, String coreQuestion,
            String unusedSummary, String content, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, title, content, status, record_type,
                                      core_question, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'NODE_RECORD', ?, ?, ?)
                """,
                id, userId, title, content == null ? "正文" : content, status,
                coreQuestion, createdAt, createdAt);
    }

    private void insertRecordWithSummary(Long id, Long userId, String aiSummary, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, content, status, record_type, ai_summary, created_at, updated_at)
                VALUES (?, ?, '正文', 'DRAFT', 'NODE_RECORD', ?, ?, ?)
                """, id, userId, aiSummary, createdAt, createdAt);
    }

    private void insertRecordWithBelief(Long id, Long userId, String beliefThen, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                INSERT INTO `record` (id, user_id, content, status, record_type, belief_then, created_at, updated_at)
                VALUES (?, ?, '正文', 'DRAFT', 'NODE_RECORD', ?, ?, ?)
                """, id, userId, beliefThen, createdAt, createdAt);
    }

    private void insertTag(Long id, String name, String status) {
        jdbcTemplate.update("""
                INSERT INTO tag (id, name, type, status, created_at)
                VALUES (?, ?, 'TOPIC', ?, ?)
                """, id, name, status, NOW);
    }

    private void bindTag(Long recordId, Long tagId) {
        jdbcTemplate.update(
                "INSERT INTO record_tag (record_id, tag_id) VALUES (?, ?)", recordId, tagId);
    }
}
