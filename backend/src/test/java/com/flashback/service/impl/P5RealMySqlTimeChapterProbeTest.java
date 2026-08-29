package com.flashback.service.impl;

import com.flashback.domain.DataDeletionScope;
import com.flashback.domain.DataOperation;
import com.flashback.domain.DataOperationStatus;
import com.flashback.domain.SealedContentPolicy;
import com.flashback.domain.TimeChapterStatus;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.CreateTimeChapterRequest;
import com.flashback.dto.TimeChapterMemberPageQuery;
import com.flashback.dto.TransferConfirmation;
import com.flashback.mapper.DataOperationMapper;
import com.flashback.service.DataOwnershipService;
import com.flashback.service.TimeChapterService;
import com.flashback.service.data.DataOwnershipArtifactStore;
import com.flashback.vo.DataOperationVO;
import com.flashback.vo.TimeChapterSummaryVO;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P5.x Gate 3a：真实 MySQL 的篇章约束、事务、数据所有权集成与清理探针。 */
@EnabledIfEnvironmentVariable(named = "P5_MYSQL_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-",
        "app.data-ownership.recovery-delay-ms=3600000",
        "app.data-ownership.cleanup-delay-ms=3600000",
        "logging.level.com.flashback.mapper=OFF"
})
@ActiveProfiles("dev")
class P5RealMySqlTimeChapterProbeTest {

    private static final long OWNER_ID = 9_950_100L;
    private static final long OTHER_ID = 9_950_200L;
    private static final long RECORD_A = 9_950_101L;
    private static final long RECORD_B = 9_950_102L;
    private static final long RECORD_C = 9_950_103L;
    private static final long OTHER_RECORD = 9_950_201L;
    private static final String OWNER_NAME = "p5-mysql-owner";
    private static final String OTHER_NAME = "p5-mysql-other";

    @Autowired private TimeChapterService chapterService;
    @Autowired private DataOwnershipService ownershipService;
    @Autowired private DataOperationMapper operationMapper;
    @Autowired private DataOwnershipArtifactStore artifactStore;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    private String artifactToken;

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
    void realMySqlMustEnforceChapterContractAndCleanupSyntheticState() throws Exception {
        assertSchema();
        assertCleanStart();
        insertUser(OWNER_ID, OWNER_NAME);
        insertUser(OTHER_ID, OTHER_NAME);
        try {
            insertRecord(RECORD_A, OWNER_ID, "SAVED", LocalDateTime.of(2026, 8, 20, 8, 0));
            insertRecord(RECORD_B, OWNER_ID, "SEALED", LocalDateTime.of(2026, 8, 21, 8, 0));
            insertRecord(RECORD_C, OWNER_ID, "UNLOCKED", LocalDateTime.of(2026, 8, 22, 8, 0));
            insertRecord(OTHER_RECORD, OTHER_ID, "SAVED", LocalDateTime.of(2026, 8, 23, 8, 0));

            TimeChapterSummaryVO source = chapterService.create(
                    OWNER_ID, create("P5 合成来源篇章", RECORD_A));
            TimeChapterSummaryVO target = chapterService.create(
                    OWNER_ID, create("P5 合成目标篇章", RECORD_B));

            assertDatabaseOwnerUniqueAndRollback(source.getId(), target.getId());

            source = chapterService.addMembers(
                    OWNER_ID, source.getId(), members(source.getVersion(), RECORD_C));
            target = chapterService.addMembers(
                    OWNER_ID, target.getId(), transfer(target.getVersion(), RECORD_A, source.getId()));
            assertThat(target.getMemberCount()).isEqualTo(2);
            assertThat(jdbc.queryForObject(
                    "SELECT chapter_id FROM time_chapter_record WHERE record_id=?",
                    Long.class, RECORD_A)).isEqualTo(target.getId());

            target = chapterService.end(OWNER_ID, target.getId(), target.getVersion());
            assertThat(target.getStatus()).isEqualTo(TimeChapterStatus.ENDED);
            assertThat(target.getEndedAt()).isNotNull();
            target = chapterService.reopen(OWNER_ID, target.getId(), target.getVersion());
            assertThat(target.getStatus()).isEqualTo(TimeChapterStatus.ACTIVE);
            assertThat(target.getEndedAt()).isNull();

            long latestSourceVersion = chapterService.detail(
                    OWNER_ID, source.getId(), new TimeChapterMemberPageQuery()).getVersion();
            chapterService.delete(OWNER_ID, source.getId(), latestSourceVersion);
            assertThat(recordCount(RECORD_C, OWNER_ID)).isOne();

            verifyExportContainsChapterWithoutDuplicatingRecordContent();
            verifySingleRecordDeleteKeepsChapter(target.getId());
            verifyClearAllRemovesOwnerChaptersAndRecords();

            assertThat(recordCount(OTHER_RECORD, OTHER_ID)).isOne();
            System.out.println("P5MYSQL PASS schema=true owner=true unique=true fk=true rollback=true "
                    + "create=true add=true transfer=true end=true reopen=true deleteChapter=true "
                    + "deleteRecord=true export=true clearAll=true externalCalls=0");
        } finally {
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P5MYSQL CLEANUP users=0 records=0 chapters=0 relations=0 operations=0 artifacts=0");
    }

    private void assertSchema() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema=DATABASE() AND table_name IN ('time_chapter','time_chapter_record')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(DISTINCT CONCAT(table_name, ':', index_name)) FROM information_schema.statistics
                WHERE table_schema=DATABASE()
                  AND ((table_name='record' AND index_name='uk_record_id_user_id')
                    OR (table_name='time_chapter' AND index_name IN
                      ('PRIMARY','uk_time_chapter_id_user_id','idx_time_chapter_user_status_updated','idx_time_chapter_user_created'))
                    OR (table_name='time_chapter_record' AND index_name IN
                      ('PRIMARY','uk_time_chapter_record_record_id','idx_time_chapter_record_user_chapter_added')))
                """, Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.referential_constraints
                WHERE constraint_schema=DATABASE()
                  AND table_name IN ('time_chapter','time_chapter_record')
                  AND delete_rule='CASCADE'
                """, Integer.class)).isEqualTo(3);
    }

    private void assertDatabaseOwnerUniqueAndRollback(long sourceId, long targetId) {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO time_chapter_record(chapter_id,record_id,user_id,added_at) VALUES (?,?,?,NOW())",
                targetId, OTHER_RECORD, OWNER_ID)).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            jdbc.update("INSERT INTO time_chapter_record(chapter_id,record_id,user_id,added_at) VALUES (?,?,?,NOW())",
                    sourceId, RECORD_C, OWNER_ID);
            jdbc.update("INSERT INTO time_chapter_record(chapter_id,record_id,user_id,added_at) VALUES (?,?,?,NOW())",
                    targetId, RECORD_C, OWNER_ID);
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM time_chapter_record WHERE record_id=?", Integer.class, RECORD_C)).isZero();
    }

    private void verifyExportContainsChapterWithoutDuplicatingRecordContent() throws Exception {
        DataOperationVO exported = ownershipService.createExport(OWNER_ID, SealedContentPolicy.RESPECT_SEAL);
        assertThat(exported.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        byte[] zip = ownershipService.downloadExport(OWNER_ID, exported.getId());
        String chapterJson = zipEntry(zip, "flashback-export/chapters/index.json");
        assertThat(chapterJson).contains("P5 合成目标篇章");
        assertThat(chapterJson).contains(String.valueOf(RECORD_A));
        assertThat(chapterJson).doesNotContain("P5 synthetic record content");
        DataOperation operation = operationMapper.selectByIdAndUserId(exported.getId(), OWNER_ID);
        artifactToken = operation.getArtifactToken();
    }

    private void verifySingleRecordDeleteKeepsChapter(long chapterId) {
        DataOperationVO intent = ownershipService.prepareDeletion(
                OWNER_ID, DataDeletionScope.RECORD, RECORD_B);
        DataOperationVO completed = ownershipService.confirmDeletion(
                OWNER_ID, intent.getId(), intent.getConfirmationText());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recordCount(RECORD_B, OWNER_ID)).isZero();
        assertThat(chapterService.detail(
                OWNER_ID, chapterId, new TimeChapterMemberPageQuery()).getMemberCount()).isEqualTo(1);
    }

    private void verifyClearAllRemovesOwnerChaptersAndRecords() {
        DataOperationVO intent = ownershipService.prepareDeletion(
                OWNER_ID, DataDeletionScope.ALL_RECORDS, null);
        DataOperationVO completed = ownershipService.confirmDeletion(
                OWNER_ID, intent.getId(), intent.getConfirmationText());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE user_id=?", Integer.class, OWNER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM time_chapter WHERE user_id=?", Integer.class, OWNER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM time_chapter_record WHERE user_id=?", Integer.class, OWNER_ID)).isZero();
    }

    private CreateTimeChapterRequest create(String name, Long... recordIds) {
        CreateTimeChapterRequest request = new CreateTimeChapterRequest();
        request.setName(name);
        request.setNote("P5 synthetic note");
        request.setRecordIds(List.of(recordIds));
        return request;
    }

    private ChangeTimeChapterMembersRequest members(long version, Long... recordIds) {
        ChangeTimeChapterMembersRequest request = new ChangeTimeChapterMembersRequest();
        request.setExpectedVersion(version);
        request.setRecordIds(List.of(recordIds));
        return request;
    }

    private ChangeTimeChapterMembersRequest transfer(long version, long recordId, long sourceId) {
        ChangeTimeChapterMembersRequest request = members(version, recordId);
        TransferConfirmation confirmation = new TransferConfirmation();
        confirmation.setRecordId(recordId);
        confirmation.setFromChapterId(sourceId);
        request.setTransfers(List.of(confirmation));
        return request;
    }

    private String zipEntry(byte[] bytes, String expectedPath) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (expectedPath.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("missing export entry: " + expectedPath);
    }

    private void assertCleanStart() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?) OR username IN (?,?)",
                Integer.class, OWNER_ID, OTHER_ID, OWNER_NAME, OTHER_NAME)).isZero();
    }

    private void insertUser(long id, String username) {
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) "
                        + "VALUES (?,?,?,'P5 MySQL probe','ENABLED',NOW(),NOW())",
                id, username, "synthetic");
    }

    private void insertRecord(long id, long userId, String status, LocalDateTime createdAt) {
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,record_type,status,created_at,updated_at) "
                        + "VALUES (?,?,?,'P5 synthetic record content','MOMENT',?,?,?)",
                id, userId, "P5 synthetic record " + id, status, createdAt, createdAt);
    }

    private int recordCount(long recordId, long userId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id=? AND user_id=?", Integer.class, recordId, userId);
    }

    private void cleanupSyntheticState() {
        if (artifactToken != null) {
            artifactStore.delete(artifactToken);
            artifactToken = null;
        }
        jdbc.update("DELETE FROM `user` WHERE id IN (?,?)", OWNER_ID, OTHER_ID);
    }

    private void assertSyntheticStateRemoved() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM time_chapter WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM time_chapter_record WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM data_operation WHERE user_id IN (?,?)", Integer.class, OWNER_ID, OTHER_ID)).isZero();
    }
}
