package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.StorageProvider;
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
class RecordAttachmentMapperIntegrationTest {

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private RecordAttachmentMapper recordAttachmentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCountSumAndSelectAvailableAttachmentsByOwnerScope() {
        Record mine = newRecord(1201L, "mine", RecordStatus.DRAFT, LocalDateTime.of(2026, 6, 18, 10, 0, 0));
        recordMapper.insert(mine);
        Record other = newRecord(9201L, "other", RecordStatus.DRAFT, LocalDateTime.of(2026, 6, 18, 11, 0, 0));
        recordMapper.insert(other);

        RecordAttachment first = attachment(mine.getId(), 1201L, RecordAttachmentType.IMAGE, "a.jpg", 100L, 1);
        RecordAttachment second = attachment(mine.getId(), 1201L, RecordAttachmentType.VOICE, "b.mp3", 200L, 2);
        RecordAttachment deleted = attachment(mine.getId(), 1201L, RecordAttachmentType.IMAGE, "deleted.jpg", 300L, 3);
        deleted.setStatus(RecordAttachmentStatus.DELETED);
        RecordAttachment otherUser = attachment(other.getId(), 9201L, RecordAttachmentType.IMAGE, "other.jpg", 400L, 1);

        recordAttachmentMapper.insert(first);
        recordAttachmentMapper.insert(second);
        recordAttachmentMapper.insert(deleted);
        recordAttachmentMapper.insert(otherUser);

        int imageCount = recordAttachmentMapper.countAvailableByRecordIdAndUserIdAndType(
                mine.getId(), 1201L, RecordAttachmentType.IMAGE);
        Long totalSize = recordAttachmentMapper.sumAvailableSizeByRecordIdAndUserId(mine.getId(), 1201L);
        List<RecordAttachment> available = recordAttachmentMapper.selectAvailableByRecordIdAndUserId(mine.getId(), 1201L);
        RecordAttachment selected = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(first.getId(), mine.getId(), 1201L);
        RecordAttachment crossUser = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(otherUser.getId(), mine.getId(), 1201L);

        assertThat(imageCount).isEqualTo(1);
        assertThat(totalSize).isEqualTo(300L);
        assertThat(available).extracting(RecordAttachment::getStorageKey).containsExactly("a.jpg", "b.mp3");
        assertThat(selected).isNotNull();
        assertThat(selected.getMimeType()).isEqualTo("image/jpeg");
        assertThat(crossUser).isNull();
    }

    private RecordAttachment attachment(
            Long recordId,
            Long userId,
            RecordAttachmentType type,
            String key,
            long sizeBytes,
            int sortOrder) {
        RecordAttachment attachment = new RecordAttachment();
        attachment.setRecordId(recordId);
        attachment.setUserId(userId);
        attachment.setType(type);
        attachment.setStorageProvider(StorageProvider.QINIU);
        attachment.setBucket("flashback-private");
        attachment.setStorageKey(key);
        attachment.setFileName(key);
        attachment.setMimeType(type == RecordAttachmentType.IMAGE ? "image/jpeg" : "audio/mpeg");
        attachment.setSizeBytes(sizeBytes);
        attachment.setSortOrder(sortOrder);
        attachment.setStatus(RecordAttachmentStatus.AVAILABLE);
        attachment.setCreatedAt(LocalDateTime.of(2026, 6, 18, 10, 0, 0).plusMinutes(sortOrder));
        attachment.setUpdatedAt(LocalDateTime.of(2026, 6, 18, 10, 0, 0).plusMinutes(sortOrder));
        return attachment;
    }

    private Record newRecord(Long userId, String title, RecordStatus status, LocalDateTime createdAt) {
        ensureUser(userId);
        Record record = new Record();
        record.setUserId(userId);
        record.setTitle(title);
        record.setContent("content-" + title);
        record.setRecordType(RecordType.NODE_RECORD);
        record.setStatus(status);
        record.setUnlockAt(createdAt.plusDays(1));
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
