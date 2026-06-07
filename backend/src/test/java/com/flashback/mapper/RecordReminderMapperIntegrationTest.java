package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordReminder;
import com.flashback.domain.RecordReminderStatus;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordReminderMapperIntegrationTest {

    private static final String TEMPLATE_TYPE_UNLOCK_REMINDER = "UNLOCK_REMINDER";

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private RecordReminderMapper recordReminderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertAndSelectRecordReminder() {
        Record record = insertRecord(8101L, "reminder-record");
        LocalDateTime now = LocalDateTime.of(2026, 3, 26, 9, 0, 0);

        RecordReminder reminder = new RecordReminder();
        reminder.setRecordId(record.getId());
        reminder.setUserId(8101L);
        reminder.setTemplateType(TEMPLATE_TYPE_UNLOCK_REMINDER);
        reminder.setReminderStatus(RecordReminderStatus.SKIPPED_NO_OPENID);
        reminder.setLastError("openid not bound");
        reminder.setCreatedAt(now);
        reminder.setUpdatedAt(now);

        int inserted = recordReminderMapper.insert(reminder);
        RecordReminder found = recordReminderMapper.selectByRecordIdAndTemplateType(
                record.getId(),
                TEMPLATE_TYPE_UNLOCK_REMINDER);

        assertThat(inserted).isEqualTo(1);
        assertThat(reminder.getId()).isNotNull();
        assertThat(found).isNotNull();
        assertThat(found.getRecordId()).isEqualTo(record.getId());
        assertThat(found.getUserId()).isEqualTo(8101L);
        assertThat(found.getTemplateType()).isEqualTo(TEMPLATE_TYPE_UNLOCK_REMINDER);
        assertThat(found.getReminderStatus()).isEqualTo(RecordReminderStatus.SKIPPED_NO_OPENID);
        assertThat(found.getLastError()).isEqualTo("openid not bound");
    }

    @Test
    void shouldEnforceOneReminderPerRecordAndTemplateType() {
        Record record = insertRecord(8102L, "dedupe-reminder-record");
        LocalDateTime now = LocalDateTime.of(2026, 3, 26, 9, 0, 0);

        RecordReminder first = newReminder(record.getId(), 8102L, now);
        RecordReminder duplicate = newReminder(record.getId(), 8102L, now.plusSeconds(1));

        recordReminderMapper.insert(first);

        assertThatThrownBy(() -> recordReminderMapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private RecordReminder newReminder(Long recordId, Long userId, LocalDateTime now) {
        RecordReminder reminder = new RecordReminder();
        reminder.setRecordId(recordId);
        reminder.setUserId(userId);
        reminder.setTemplateType(TEMPLATE_TYPE_UNLOCK_REMINDER);
        reminder.setReminderStatus(RecordReminderStatus.SEND_PENDING);
        reminder.setCreatedAt(now);
        reminder.setUpdatedAt(now);
        return reminder;
    }

    private Record insertRecord(Long userId, String title) {
        ensureUser(userId);
        Record record = new Record();
        record.setUserId(userId);
        record.setTitle(title);
        record.setContent("content-" + title);
        record.setRecordType(RecordType.NODE_RECORD);
        record.setStatus(RecordStatus.UNLOCKED);
        record.setUnlockAt(LocalDateTime.of(2026, 3, 25, 10, 0, 0));
        record.setUnlockedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
        record.setCreatedAt(LocalDateTime.of(2026, 3, 24, 9, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
        recordMapper.insert(record);
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

        LocalDateTime now = LocalDateTime.of(2026, 3, 24, 8, 0, 0);
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
