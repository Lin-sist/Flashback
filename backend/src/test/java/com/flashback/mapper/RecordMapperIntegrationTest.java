package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
                RecordType.EMOTION_NOTE, "core-updated", unlockAt, updatedAt);
        int updatedSealed = recordMapper.updateDraftByIdAndUserId(
                sealed.getId(), 1002L, "sealed-updated", "content-updated",
                RecordType.EMOTION_NOTE, "core-updated", unlockAt, updatedAt);

        assertThat(updatedDraft).isEqualTo(1);
        assertThat(updatedSealed).isEqualTo(0);

        Record updatedRecord = recordMapper.selectByIdAndUserId(draft.getId(), 1002L);
        assertThat(updatedRecord).isNotNull();
        assertThat(updatedRecord.getTitle()).isEqualTo("draft-updated");
        assertThat(updatedRecord.getContent()).isEqualTo("content-updated");
        assertThat(updatedRecord.getRecordType()).isEqualTo(RecordType.EMOTION_NOTE);
        assertThat(updatedRecord.getCoreQuestion()).isEqualTo("core-updated");
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

        long countDraftNode = recordMapper.countByUserAndCondition(1004L, RecordStatus.DRAFT, RecordType.NODE_RECORD);
        assertThat(countDraftNode).isEqualTo(2L);

        List<Record> page = recordMapper.selectPageByUserAndCondition(
                1004L, RecordStatus.DRAFT, RecordType.NODE_RECORD, 0, 10);
        assertThat(page).hasSize(2);
        assertThat(page.get(0).getTitle()).isEqualTo("newer");
        assertThat(page.get(1).getTitle()).isEqualTo("older");

        List<Record> secondPage = recordMapper.selectPageByUserAndCondition(
                1004L, RecordStatus.DRAFT, RecordType.NODE_RECORD, 1, 1);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).getTitle()).isEqualTo("older");
    }

    private Record newRecord(Long userId, String title, RecordStatus status, RecordType recordType,
            LocalDateTime createdAt) {
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
}
