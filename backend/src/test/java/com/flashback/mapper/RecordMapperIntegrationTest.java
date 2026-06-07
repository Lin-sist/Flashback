package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.LifeNodeType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordMapperIntegrationTest {

        @Autowired
        private RecordMapper recordMapper;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        void shouldInsertAndSelectByIdAndUserId() {
                Record draft = newRecord(1001L, "first", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 1, 10, 0, 0));

                int inserted = recordMapper.insert(draft);

                assertThat(inserted).isEqualTo(1);
                assertThat(draft.getId()).isNotNull();

                Record found = recordMapper.selectByIdAndUserId(draft.getId(), 1001L);
                assertThat(found).isNotNull();
                assertThat(found.getTitle()).isEqualTo("first");
                assertThat(found.getStatus()).isEqualTo(RecordStatus.DRAFT);
                assertThat(found.getRecordType()).isEqualTo(RecordType.NODE_RECORD);

                Record notFound = recordMapper.selectByIdAndUserId(draft.getId(), 9999L);
                assertThat(notFound).isNull();
        }

        @Test
        void updateDraftByIdAndUserIdShouldOnlyAffectDraft() {
                Record draft = newRecord(1002L, "draft-title", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 2, 10, 0, 0));
                recordMapper.insert(draft);

                Record sealed = newRecord(1002L, "sealed-title", RecordStatus.SEALED, RecordType.FUTURE_LETTER,
                                LocalDateTime.of(2026, 2, 2, 11, 0, 0));
                recordMapper.insert(sealed);

                LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 2, 12, 0, 0);
                LocalDateTime unlockAt = LocalDateTime.of(2026, 3, 1, 8, 0, 0);

                int updatedDraft = recordMapper.updateDraftByIdAndUserId(
                                draft.getId(), 1002L, "draft-updated", "content-updated",
                                RecordType.EMOTION_NOTE, "core-updated", "summary-updated",
                                "[\"prompt-a\",\"prompt-b\"]", "belief-updated",
                                LifeNodeType.OTHER, "自定义节点", unlockAt, updatedAt);
                int updatedSealed = recordMapper.updateDraftByIdAndUserId(
                                sealed.getId(), 1002L, "sealed-updated", "content-updated",
                                RecordType.EMOTION_NOTE, "core-updated", "summary-updated",
                                "[\"prompt-a\",\"prompt-b\"]", "belief-updated",
                                LifeNodeType.OTHER, "自定义节点", unlockAt, updatedAt);

                assertThat(updatedDraft).isEqualTo(1);
                assertThat(updatedSealed).isEqualTo(0);

                Record updatedRecord = recordMapper.selectByIdAndUserId(draft.getId(), 1002L);
                assertThat(updatedRecord).isNotNull();
                assertThat(updatedRecord.getTitle()).isEqualTo("draft-updated");
                assertThat(updatedRecord.getContent()).isEqualTo("content-updated");
                assertThat(updatedRecord.getRecordType()).isEqualTo(RecordType.EMOTION_NOTE);
                assertThat(updatedRecord.getCoreQuestion()).isEqualTo("core-updated");
                assertThat(updatedRecord.getAiSummary()).isEqualTo("summary-updated");
                assertThat(updatedRecord.getAiPromptResult()).isEqualTo("[\"prompt-a\",\"prompt-b\"]");
                assertThat(updatedRecord.getBeliefThen()).isEqualTo("belief-updated");
                assertThat(updatedRecord.getLifeNodeType()).isEqualTo(LifeNodeType.OTHER);
                assertThat(updatedRecord.getLifeNodeCustomLabel()).isEqualTo("自定义节点");
                assertThat(updatedRecord.getUnlockAt()).isEqualTo(unlockAt);
                assertThat(updatedRecord.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        void sealDraftByIdAndUserIdShouldOnlyAffectDraft() {
                Record draft = newRecord(1003L, "draft-to-seal", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 3, 9, 0, 0));
                recordMapper.insert(draft);

                Record unlocked = newRecord(1003L, "already-unlocked", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 3, 9, 30, 0));
                recordMapper.insert(unlocked);

                LocalDateTime sealedAt = LocalDateTime.of(2026, 2, 3, 10, 0, 0);
                LocalDateTime updatedAt = LocalDateTime.of(2026, 2, 3, 10, 1, 0);

                int sealedDraft = recordMapper.sealDraftByIdAndUserId(draft.getId(), 1003L, sealedAt, updatedAt);
                int sealedUnlocked = recordMapper.sealDraftByIdAndUserId(unlocked.getId(), 1003L, sealedAt, updatedAt);

                assertThat(sealedDraft).isEqualTo(1);
                assertThat(sealedUnlocked).isEqualTo(0);

                Record sealedRecord = recordMapper.selectByIdAndUserId(draft.getId(), 1003L);
                assertThat(sealedRecord).isNotNull();
                assertThat(sealedRecord.getStatus()).isEqualTo(RecordStatus.SEALED);
                assertThat(sealedRecord.getSealedAt()).isEqualTo(sealedAt);
                assertThat(sealedRecord.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        void shouldCountAndSelectPageByUserAndCondition() {
                recordMapper.insert(newRecord(1004L, "older", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 4, 9, 0, 0)));
                recordMapper.insert(newRecord(1004L, "newer", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 4, 10, 0, 0)));
                recordMapper.insert(newRecord(1004L, "sealed", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 4, 11, 0, 0)));
                recordMapper.insert(newRecord(1004L, "other-type", RecordStatus.DRAFT, RecordType.FUTURE_LETTER,
                                LocalDateTime.of(2026, 2, 4, 12, 0, 0)));
                recordMapper.insert(newRecord(2004L, "other-user", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 2, 4, 13, 0, 0)));

                long countDraftNode = recordMapper.countByUserAndCondition(
                                1004L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                null,
                                null);
                assertThat(countDraftNode).isEqualTo(2L);

                List<Record> page = recordMapper.selectPageByUserAndCondition(
                                1004L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                null,
                                null,
                                0,
                                10);
                assertThat(page).hasSize(2);
                assertThat(page.get(0).getTitle()).isEqualTo("newer");
                assertThat(page.get(1).getTitle()).isEqualTo("older");

                List<Record> secondPage = recordMapper.selectPageByUserAndCondition(
                                1004L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                null,
                                null,
                                1,
                                1);
                assertThat(secondPage).hasSize(1);
                assertThat(secondPage.get(0).getTitle()).isEqualTo("older");
        }

        @Test
        void shouldUseIdDescTieBreakerWhenCreatedAtIsSame() {
                LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 2, 5, 9, 0, 0);
                Record first = newRecord(1005L, "same-time-first", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                sameCreatedAt);
                recordMapper.insert(first);
                Record second = newRecord(1005L, "same-time-second", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                sameCreatedAt);
                recordMapper.insert(second);

                List<Record> page = recordMapper.selectPageByUserAndCondition(
                                1005L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                null,
                                null,
                                0,
                                10);

                assertThat(page).hasSize(2);
                assertThat(page.get(0).getId()).isEqualTo(second.getId());
                assertThat(page.get(1).getId()).isEqualTo(first.getId());
        }

        @Test
        void shouldFilterRecordPageByTagAndKeyword() {
                Record target = newRecord(4001L, "实习焦虑", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 4, 1, 9, 0, 0));
                recordMapper.insert(target);

                Record other = newRecord(4001L, "毕业总结", RecordStatus.DRAFT, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 4, 2, 9, 0, 0));
                recordMapper.insert(other);

                long moodTagId = insertTag("焦虑", "MOOD", "ENABLED", LocalDateTime.of(2026, 3, 1, 0, 0, 0));
                long topicTagId = insertTag("实习", "TOPIC", "ENABLED", LocalDateTime.of(2026, 3, 1, 0, 0, 1));
                bindRecordTag(target.getId(), moodTagId);
                bindRecordTag(target.getId(), topicTagId);
                bindRecordTag(other.getId(), topicTagId);

                long total = recordMapper.countByUserAndCondition(
                                4001L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                moodTagId,
                                "实习");
                List<Record> page = recordMapper.selectPageByUserAndCondition(
                                4001L,
                                RecordStatus.DRAFT,
                                RecordType.NODE_RECORD,
                                moodTagId,
                                "实习",
                                0,
                                10);

                assertThat(total).isEqualTo(1L);
                assertThat(page).hasSize(1);
                assertThat(page.get(0).getId()).isEqualTo(target.getId());
        }

        @Test
        void shouldFilterRecordPageBySharedTagWithoutLeakingOtherUsers() {
                long sharedTagId = insertTag("阶段复盘", "TOPIC", "ENABLED",
                                LocalDateTime.of(2026, 4, 3, 0, 0, 0));

                Record mine = newRecord(4010L, "我的阶段复盘", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 4, 3, 9, 0, 0));
                recordMapper.insert(mine);
                bindRecordTag(mine.getId(), sharedTagId);

                Record otherUser = newRecord(9010L, "别人的阶段复盘", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 4, 3, 10, 0, 0));
                recordMapper.insert(otherUser);
                bindRecordTag(otherUser.getId(), sharedTagId);

                long total = recordMapper.countByUserAndCondition(
                                4010L,
                                RecordStatus.SEALED,
                                null,
                                sharedTagId,
                                "阶段");
                List<Record> page = recordMapper.selectPageByUserAndCondition(
                                4010L,
                                RecordStatus.SEALED,
                                null,
                                sharedTagId,
                                "阶段",
                                0,
                                10);

                assertThat(total).isEqualTo(1L);
                assertThat(page).hasSize(1);
                assertThat(page.get(0).getId()).isEqualTo(mine.getId());
                assertThat(page.get(0).getUserId()).isEqualTo(4010L);
        }

        @Test
        void shouldSelectExpiredSealedRecordsOnly() {
                LocalDateTime now = LocalDateTime.of(2026, 3, 26, 16, 0, 0);
                recordMapper.insert(newRecord(3001L, "expired-sealed", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 20, 10, 0, 0)));

                Record notExpired = newRecord(3001L, "not-expired", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 20, 11, 0, 0));
                notExpired.setUnlockAt(now.plusHours(1));
                recordMapper.insert(notExpired);

                recordMapper.insert(newRecord(3001L, "already-unlocked", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 20, 12, 0, 0)));

                List<Record> expired = recordMapper.selectExpiredSealedRecords(now, 10);
                assertThat(expired).hasSize(1);
                assertThat(expired.get(0).getTitle()).isEqualTo("expired-sealed");
        }

        @Test
        void unlockSealedByIdShouldBeIdempotent() {
                Record sealed = newRecord(3002L, "to-unlock", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 21, 9, 0, 0));
                recordMapper.insert(sealed);

                LocalDateTime now = LocalDateTime.of(2026, 3, 26, 16, 0, 0);
                int first = recordMapper.unlockSealedById(sealed.getId(), now, now);
                int second = recordMapper.unlockSealedById(sealed.getId(), now.plusMinutes(1), now.plusMinutes(1));

                assertThat(first).isEqualTo(1);
                assertThat(second).isEqualTo(0);

                Record unlocked = recordMapper.selectByIdAndUserId(sealed.getId(), 3002L);
                assertThat(unlocked).isNotNull();
                assertThat(unlocked.getStatus()).isEqualTo(RecordStatus.UNLOCKED);
                assertThat(unlocked.getUnlockedAt()).isEqualTo(now);
        }

        @Test
        void updateLaterReflectionShouldOnlyAffectUnlockedWithinSubmitLimit() {
                Record unlocked = newRecord(3005L, "later-ok", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 24, 10, 0, 0));
                unlocked.setRealityLaterSubmitCount(1);
                recordMapper.insert(unlocked);

                Record sealed = newRecord(3005L, "later-sealed", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 24, 11, 0, 0));
                recordMapper.insert(sealed);

                Record exhausted = newRecord(3005L, "later-exhausted", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 24, 12, 0, 0));
                exhausted.setRealityLaterSubmitCount(2);
                recordMapper.insert(exhausted);

                LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 27, 10, 0, 0);

                int updatedUnlocked = recordMapper.updateLaterReflectionByIdAndUserId(
                                unlocked.getId(), 3005L, "后来其实我更需要慢一点", updatedAt);
                int updatedSealed = recordMapper.updateLaterReflectionByIdAndUserId(
                                sealed.getId(), 3005L, "sealed should fail", updatedAt);
                int updatedExhausted = recordMapper.updateLaterReflectionByIdAndUserId(
                                exhausted.getId(), 3005L, "exhausted should fail", updatedAt);

                assertThat(updatedUnlocked).isEqualTo(1);
                assertThat(updatedSealed).isEqualTo(0);
                assertThat(updatedExhausted).isEqualTo(0);

                Record found = recordMapper.selectByIdAndUserId(unlocked.getId(), 3005L);
                assertThat(found.getRealityLater()).isEqualTo("后来其实我更需要慢一点");
                assertThat(found.getRealityLaterSubmitCount()).isEqualTo(2);
                assertThat(found.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        void shouldPageUnlockedRecordsByUserOnly() {
                Record unlockedOld = newRecord(3003L, "unlocked-old", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 22, 10, 0, 0));
                unlockedOld.setUnlockedAt(LocalDateTime.of(2026, 3, 25, 9, 0, 0));
                recordMapper.insert(unlockedOld);

                Record unlockedNew = newRecord(3003L, "unlocked-new", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 22, 11, 0, 0));
                unlockedNew.setUnlockedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
                recordMapper.insert(unlockedNew);

                recordMapper.insert(newRecord(3003L, "sealed-record", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 22, 12, 0, 0)));
                recordMapper.insert(newRecord(9003L, "other-user", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 22, 13, 0, 0)));

                long total = recordMapper.countUnlockedByUser(3003L);
                List<Record> page = recordMapper.selectUnlockedPageByUser(3003L, 0, 10);

                assertThat(total).isEqualTo(2);
                assertThat(page).hasSize(2);
                assertThat(page.get(0).getTitle()).isEqualTo("unlocked-new");
                assertThat(page.get(1).getTitle()).isEqualTo("unlocked-old");
        }

        @Test
        void shouldUseIdDescTieBreakerWhenUnlockedAtIsSame() {
                LocalDateTime sameUnlockedAt = LocalDateTime.of(2026, 3, 27, 9, 0, 0);

                Record first = newRecord(3004L, "unlocked-same-first", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 23, 10, 0, 0));
                first.setUnlockedAt(sameUnlockedAt);
                recordMapper.insert(first);

                Record second = newRecord(3004L, "unlocked-same-second", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 23, 11, 0, 0));
                second.setUnlockedAt(sameUnlockedAt);
                recordMapper.insert(second);

                recordMapper.insert(newRecord(9004L, "other-user-unlocked", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 23, 12, 0, 0)));

                List<Record> page = recordMapper.selectUnlockedPageByUser(3004L, 0, 10);

                assertThat(page).hasSize(2);
                assertThat(page.get(0).getId()).isEqualTo(second.getId());
                assertThat(page.get(1).getId()).isEqualTo(first.getId());
        }

        @Test
        void shouldSelectTimelineByYearAndTag() {
                Record april = newRecord(5001L, "april-note", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 4, 2, 10, 0, 0));
                recordMapper.insert(april);

                Record march = newRecord(5001L, "march-note", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 3, 20, 10, 0, 0));
                recordMapper.insert(march);

                long topicTagId = insertTag("求职", "TOPIC", "ENABLED", LocalDateTime.of(2026, 3, 1, 0, 0, 0));
                bindRecordTag(april.getId(), topicTagId);

                List<Record> timeline = recordMapper.selectTimelineByUserAndCondition(5001L, topicTagId, 2026);
                assertThat(timeline).hasSize(1);
                assertThat(timeline.get(0).getId()).isEqualTo(april.getId());
        }

        @Test
        void shouldUseIdDescTieBreakerAndUserScopeForTimelineWhenCreatedAtIsSame() {
                LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);

                Record first = newRecord(5002L, "timeline-same-first", RecordStatus.SEALED, RecordType.NODE_RECORD,
                                sameCreatedAt);
                recordMapper.insert(first);

                Record second = newRecord(5002L, "timeline-same-second", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                sameCreatedAt);
                recordMapper.insert(second);

                recordMapper.insert(newRecord(9002L, "other-user-newer", RecordStatus.UNLOCKED, RecordType.NODE_RECORD,
                                LocalDateTime.of(2026, 5, 2, 10, 0, 0)));

                List<Record> timeline = recordMapper.selectTimelineByUserAndCondition(5002L, null, 2026);

                assertThat(timeline).hasSize(2);
                assertThat(timeline.get(0).getId()).isEqualTo(second.getId());
                assertThat(timeline.get(1).getId()).isEqualTo(first.getId());
                assertThat(timeline).allSatisfy(record -> assertThat(record.getUserId()).isEqualTo(5002L));
        }

        private long insertTag(String name, String type, String status, LocalDateTime createdAt) {
                jdbcTemplate.update(
                                "INSERT INTO tag (name, type, status, created_at) VALUES (?, ?, ?, ?)",
                                name,
                                type,
                                status,
                                createdAt);
                Long id = jdbcTemplate.queryForObject(
                                "SELECT id FROM tag WHERE name = ? AND type = ?",
                                Long.class,
                                name,
                                type);
                assertThat(id).isNotNull();
                return id;
        }

        private void bindRecordTag(Long recordId, Long tagId) {
                jdbcTemplate.update(
                                "INSERT INTO record_tag (record_id, tag_id) VALUES (?, ?)",
                                recordId,
                                tagId);
        }

        private Record newRecord(Long userId, String title, RecordStatus status, RecordType recordType,
                        LocalDateTime createdAt) {
                ensureUser(userId);
                Record record = new Record();
                record.setUserId(userId);
                record.setTitle(title);
                record.setContent("content-" + title);
                record.setRecordType(recordType);
                record.setCoreQuestion("core-" + title);
                record.setStatus(status);
                record.setUnlockAt(createdAt.plusDays(1));
                record.setSealedAt(status == RecordStatus.SEALED ? createdAt.plusHours(1) : null);
                record.setUnlockedAt(status == RecordStatus.UNLOCKED ? createdAt.plusHours(2) : null);
                record.setAiSummary("ai-summary-" + title);
                record.setAiPromptResult("ai-prompt-" + title);
                record.setCreatedAt(createdAt);
                record.setUpdatedAt(createdAt);
                return record;
        }

        private void ensureUser(Long userId) {
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(1) FROM `user` WHERE id = ?",
                                Integer.class,
                                userId);
                if (count != null && count > 0) {
                        return;
                }

                LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
                jdbcTemplate.update(
                                """
                                                INSERT INTO `user` (
                                                    id,
                                                    username,
                                                    password_hash,
                                                    nickname,
                                                    status,
                                                    created_at,
                                                    updated_at
                                                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                                                """,
                                userId,
                                "user_" + userId,
                                "test-password-hash",
                                "User-" + userId,
                                "ENABLED",
                                now,
                                now);
        }
}
