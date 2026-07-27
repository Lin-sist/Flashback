package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.mapper.UserMapper;
import com.flashback.domain.User;
import com.flashback.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * C2 追加语义集成测试（真实 H2 + MyBatis）。
 *
 * 覆盖核心承诺：
 * - 正文只追加不覆写，既有正文逐字保留；
 * - 标签只追加不清空，重复不产生重复绑定；
 * - 设置解锁时间不触发封存；
 * - 封存后一律拒绝；跨用户拒绝。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecordServiceAppendIntegrationTest {

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private RecordTagMapper recordTagMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        userId = createUser("append-owner");
        otherUserId = createUser("append-stranger");
        insertTag(TAG_EXISTING, "已有标签");
        insertTag(TAG_NEW, "新增标签");
        insertTag(TAG_THIRD, "第三标签");
    }

    private static final Long TAG_EXISTING = 8801L;
    private static final Long TAG_NEW = 8802L;
    private static final Long TAG_THIRD = 8803L;

    private void insertTag(Long id, String name) {
        jdbcTemplate.update(
                "INSERT INTO tag (id, name, type, status, created_at) VALUES (?, ?, ?, ?, ?)",
                id, name, "MOOD", "ENABLED", LocalDateTime.now());
    }

    // ---------- 正文追加 ----------

    @Test
    void shouldAppendContentWithoutOverwritingExistingText() {
        Long recordId = createDraft("原本写下的那一段");

        recordService.appendContent(userId, recordId, "后来补的这一段");

        String content = recordMapper.selectByIdAndUserId(recordId, userId).getContent();
        // 逐字保留原文，且原文仍在前。
        assertThat(content).startsWith("原本写下的那一段");
        assertThat(content).contains("后来补的这一段");
        assertThat(content).isEqualTo("原本写下的那一段\n\n后来补的这一段");
    }

    @Test
    void shouldAppendContentWhenDraftBodyEmpty() {
        Long recordId = createDraftWithNullContent();

        recordService.appendContent(userId, recordId, "第一段");

        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent()).isEqualTo("第一段");
    }

    @Test
    void shouldRejectAppendContentOnSealedRecord() {
        Long recordId = createDraft("正文");
        sealDirectly(recordId);

        assertThatThrownBy(() -> recordService.appendContent(userId, recordId, "还想加"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已封存");

        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent()).isEqualTo("正文");
    }

    @Test
    void shouldRejectAppendContentForOtherUser() {
        Long recordId = createDraft("正文");

        assertThatThrownBy(() -> recordService.appendContent(otherUserId, recordId, "偷偷加"))
                .isInstanceOf(RuntimeException.class);

        assertThat(recordMapper.selectByIdAndUserId(recordId, userId).getContent()).isEqualTo("正文");
    }

    @Test
    void shouldRejectBlankAppendContent() {
        Long recordId = createDraft("正文");

        assertThatThrownBy(() -> recordService.appendContent(userId, recordId, "   "))
                .isInstanceOf(BizException.class);
    }

    // ---------- 标签追加 ----------

    @Test
    void shouldAppendTagsKeepingExistingOnes() {
        Long recordId = createDraft("正文");
        recordTagMapper.batchInsert(recordId, List.of(TAG_EXISTING));

        recordService.appendTags(userId, recordId, List.of(TAG_NEW));

        List<Long> tagIds = recordTagMapper.selectTagIdsByRecordId(recordId);
        // 既有标签必须保留，这是「只追加不清空」的核心承诺。
        assertThat(tagIds).contains(TAG_EXISTING, TAG_NEW);
    }

    @Test
    void shouldNotDuplicateTagBindingOnRepeatedAppend() {
        Long recordId = createDraft("正文");
        recordTagMapper.batchInsert(recordId, List.of(TAG_EXISTING));

        recordService.appendTags(userId, recordId, List.of(TAG_EXISTING));
        recordService.appendTags(userId, recordId, List.of(TAG_EXISTING));

        assertThat(recordTagMapper.selectTagIdsByRecordId(recordId)).containsExactly(TAG_EXISTING);
    }

    @Test
    void shouldRejectUnknownTagId() {
        Long recordId = createDraft("正文");

        assertThatThrownBy(() -> recordService.appendTags(userId, recordId, List.of(999999L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    void shouldRejectAppendTagsOnSealedRecord() {
        Long recordId = createDraft("正文");
        recordTagMapper.batchInsert(recordId, List.of(TAG_EXISTING));
        sealDirectly(recordId);

        assertThatThrownBy(() -> recordService.appendTags(userId, recordId, List.of(TAG_NEW)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已封存");

        assertThat(recordTagMapper.selectTagIdsByRecordId(recordId)).containsExactly(TAG_EXISTING);
    }

    @Test
    void shouldRejectEmptyTagAppend() {
        Long recordId = createDraft("正文");

        assertThatThrownBy(() -> recordService.appendTags(userId, recordId, List.of()))
                .isInstanceOf(BizException.class);
    }

    // ---------- 解锁时间 ----------

    @Test
    void shouldSetUnlockAtWithoutSealing() {
        Long recordId = createDraft("正文");
        LocalDateTime unlockAt = LocalDateTime.now().plusYears(1).withNano(0);

        recordService.updateUnlockAt(userId, recordId, unlockAt);

        Record updated = recordMapper.selectByIdAndUserId(recordId, userId);
        assertThat(updated.getUnlockAt()).isNotNull();
        // 关键：仍是草稿，封存必须由用户自己确认。
        assertThat(updated.getStatus()).isEqualTo(RecordStatus.DRAFT);
        assertThat(updated.getSealedAt()).isNull();
    }

    @Test
    void shouldRejectPastUnlockAt() {
        Long recordId = createDraft("正文");

        assertThatThrownBy(() -> recordService.updateUnlockAt(
                userId, recordId, LocalDateTime.now().minusDays(1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("必须晚于当前时间");
    }

    @Test
    void shouldRejectUnlockAtOnSealedRecord() {
        Long recordId = createDraft("正文");
        sealDirectly(recordId);

        assertThatThrownBy(() -> recordService.updateUnlockAt(
                userId, recordId, LocalDateTime.now().plusYears(1)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已封存");
    }

    // ---------- helpers ----------

    private Long createUser(String suffix) {
        long unique = System.nanoTime();
        User user = new User();
        user.setUsername(suffix + "_" + unique);
        user.setPasswordHash("hash_" + unique);
        user.setNickname(suffix);
        user.setEmail(suffix + unique + "@test.com");
        user.setOpenid("openid-" + suffix + "-" + unique);
        user.setStatus(com.flashback.domain.UserStatus.ENABLED);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return user.getId();
    }

    private Long createDraft(String content) {
        Record record = baseDraft();
        record.setContent(content);
        recordMapper.insert(record);
        return record.getId();
    }

    private Long createDraftWithNullContent() {
        Record record = baseDraft();
        record.setContent("");
        recordMapper.insert(record);
        return record.getId();
    }

    private Record baseDraft() {
        Record record = new Record();
        record.setUserId(userId);
        record.setTitle("测试草稿");
        record.setRecordType(com.flashback.domain.RecordType.EMOTION_NOTE);
        record.setStatus(RecordStatus.DRAFT);
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private void sealDirectly(Long recordId) {
        LocalDateTime now = LocalDateTime.now();
        recordMapper.updateDraftUnlockAtByIdAndUserId(recordId, userId, now.plusYears(1), now);
        recordMapper.sealDraftByIdAndUserId(recordId, userId, now, now);
    }
}
