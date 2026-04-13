package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.UnlockNoticeLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnlockNoticeLogMapperIntegrationTest {

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UnlockNoticeLogMapper unlockNoticeLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertUnlockNoticeLog() {
        ensureUser(8001L);
        Record sealed = new Record();
        sealed.setUserId(8001L);
        sealed.setTitle("to-unlock");
        sealed.setContent("content");
        sealed.setRecordType(RecordType.NODE_RECORD);
        sealed.setStatus(RecordStatus.SEALED);
        sealed.setUnlockAt(LocalDateTime.of(2026, 3, 25, 10, 0, 0));
        sealed.setCreatedAt(LocalDateTime.of(2026, 3, 24, 9, 0, 0));
        sealed.setUpdatedAt(LocalDateTime.of(2026, 3, 24, 9, 0, 0));
        recordMapper.insert(sealed);

        UnlockNoticeLog log = new UnlockNoticeLog();
        log.setRecordId(sealed.getId());
        log.setUserId(8001L);
        log.setNoticeType("SYSTEM_UNLOCK");
        log.setNoticeStatus("SUCCESS");
        log.setCreatedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));

        int inserted = unlockNoticeLogMapper.insert(log);
        assertThat(inserted).isEqualTo(1);
        assertThat(log.getId()).isNotNull();
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
