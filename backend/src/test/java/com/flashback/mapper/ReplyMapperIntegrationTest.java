package com.flashback.mapper;

import com.flashback.domain.Reply;
import com.flashback.domain.ReplyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
class ReplyMapperIntegrationTest {

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertAndSelectByRecordId() {
        ensureRecord(1001L, 2001L);
        Reply reply = newReply(1001L, 2001L, "你已经走了很远");

        int inserted = replyMapper.insert(reply);

        assertThat(inserted).isEqualTo(1);
        assertThat(reply.getId()).isNotNull();

        Reply found = replyMapper.selectByRecordId(1001L);
        assertThat(found).isNotNull();
        assertThat(found.getContent()).isEqualTo("你已经走了很远");
        assertThat(found.getReplyType()).isEqualTo(ReplyType.SHORT_REPLY);
    }

    @Test
    void shouldReturnNullWhenReplyNotExists() {
        Reply found = replyMapper.selectByRecordId(9999L);
        assertThat(found).isNull();
    }

    @Test
    void shouldEnforceUniqueRecordId() {
        ensureRecord(1002L, 2002L);
        replyMapper.insert(newReply(1002L, 2002L, "first"));

        assertThatThrownBy(() -> replyMapper.insert(newReply(1002L, 2002L, "second")))
                .isInstanceOfAny(DuplicateKeyException.class, DataIntegrityViolationException.class);
    }

    @Test
    void shouldInsertWhenContentLengthIsExactly500() {
        String content500 = "a".repeat(500);
        ensureRecord(1003L, 2003L);

        Reply reply = newReply(1003L, 2003L, content500);
        int inserted = replyMapper.insert(reply);

        assertThat(inserted).isEqualTo(1);
        Reply found = replyMapper.selectByRecordId(1003L);
        assertThat(found).isNotNull();
        assertThat(found.getContent()).hasSize(500);
    }

    private Reply newReply(Long recordId, Long userId, String content) {
        Reply reply = new Reply();
        reply.setRecordId(recordId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setReplyType(ReplyType.SHORT_REPLY);
        reply.setCreatedAt(LocalDateTime.of(2026, 4, 9, 20, 0, 0));
        return reply;
    }

    private void ensureRecord(Long recordId, Long userId) {
        ensureUser(userId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM `record` WHERE id = ?",
                Integer.class,
                recordId);
        if (count != null && count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0, 0);
        jdbcTemplate.update(
                """
                        INSERT INTO `record` (
                            id,
                            user_id,
                            title,
                            content,
                            record_type,
                            status,
                            unlock_at,
                            created_at,
                            updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                recordId,
                userId,
                "record-" + recordId,
                "content-" + recordId,
                "NODE_RECORD",
                "UNLOCKED",
                now.plusDays(1),
                now,
                now);
    }

    private void ensureUser(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM `user` WHERE id = ?",
                Integer.class,
                userId);
        if (count != null && count > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 9, 0, 0);
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
