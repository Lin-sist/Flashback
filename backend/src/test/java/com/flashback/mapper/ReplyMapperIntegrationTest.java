package com.flashback.mapper;

import com.flashback.domain.Reply;
import com.flashback.domain.ReplyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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

    @Test
    void shouldInsertAndSelectByRecordId() {
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
        replyMapper.insert(newReply(1002L, 2002L, "first"));

        assertThatThrownBy(() -> replyMapper.insert(newReply(1002L, 2002L, "second")))
                .isInstanceOfAny(DuplicateKeyException.class, DataIntegrityViolationException.class);
    }

    @Test
    void shouldInsertWhenContentLengthIsExactly500() {
        String content500 = "a".repeat(500);

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
}
