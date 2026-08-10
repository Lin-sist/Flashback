package com.flashback.service.impl;

import com.flashback.domain.Record;
import com.flashback.common.exception.BizException;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PresentMomentCaptureIntegrationTest {

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                """
                        INSERT INTO `user` (username, password_hash, nickname, status, created_at, updated_at)
                        VALUES (?, ?, ?, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                "p31-owner",
                "test-password-hash",
                "P3.1 Owner");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = ?",
                Long.class,
                "p31-owner");
    }

    @Test
    void shouldCreateRecoverableMomentDraftWithoutText() {
        LocalDateTime beforeCreate = LocalDateTime.now();

        var created = recordService.create(userId, new CreateRecordRequest());

        assertThat(created.getStatus()).isEqualTo(RecordStatus.DRAFT);
        assertThat(created.getRecordType()).isEqualTo(RecordType.MOMENT);
        assertThat(created.getContent()).isEmpty();

        Record persisted = recordMapper.selectByIdAndUserId(created.getId(), userId);
        assertThat(persisted.getDraftExpiresAt())
                .isAfter(beforeCreate.plusDays(6))
                .isBefore(beforeCreate.plusDays(8));
    }

    @Test
    void shouldExplicitlySaveTextOnlyDraft() {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent("今天在回家路上看见了晚霞");
        var draft = recordService.create(userId, request);

        var saved = recordService.save(userId, draft.getId());

        assertThat(saved.getStatus()).isEqualTo(RecordStatus.SAVED);
        Record persisted = recordMapper.selectByIdAndUserId(draft.getId(), userId);
        assertThat(persisted.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(persisted.getDraftExpiresAt()).isNull();
    }

    @Test
    void shouldSaveImageOnlyDraftAfterAttachmentIsAvailable() {
        var draft = recordService.create(userId, new CreateRecordRequest());
        insertAttachment(draft.getId(), "IMAGE", "AVAILABLE", "image-only.jpg");

        var saved = recordService.save(userId, draft.getId());

        assertThat(saved.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(saved.getContent()).isEmpty();
        assertThat(saved.getAttachments()).hasSize(1);
    }

    @Test
    void shouldSaveVoiceOnlyDraftAfterAttachmentIsAvailable() {
        var draft = recordService.create(userId, new CreateRecordRequest());
        insertAttachment(draft.getId(), "VOICE", "AVAILABLE", "voice-only.mp3");

        var saved = recordService.save(userId, draft.getId());

        assertThat(saved.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(saved.getContent()).isEmpty();
        assertThat(saved.getAttachments()).hasSize(1);
    }

    @Test
    void shouldRejectDraftWithOnlyPendingMedia() {
        var draft = recordService.create(userId, new CreateRecordRequest());
        insertAttachment(draft.getId(), "IMAGE", "PENDING", "pending.jpg");

        assertThatThrownBy(() -> recordService.save(userId, draft.getId()))
                .isInstanceOf(BizException.class)
                .hasMessage("至少留下一句话、一张图片或一段声音");

        assertThat(recordMapper.selectByIdAndUserId(draft.getId(), userId).getStatus())
                .isEqualTo(RecordStatus.DRAFT);
    }

    @Test
    void shouldTreatRepeatedSaveAsIdempotentSuccess() {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent("已经成立的一句话");
        var draft = recordService.create(userId, request);

        var first = recordService.save(userId, draft.getId());
        var second = recordService.save(userId, draft.getId());

        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(second.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id = ?",
                Integer.class,
                draft.getId())).isEqualTo(1);
    }

    @Test
    void shouldRejectSealingBeforeExplicitSave() {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent("先保存，再决定是否交给时间");
        request.setUnlockAt(LocalDateTime.now().plusDays(30));
        var draft = recordService.create(userId, request);

        assertThatThrownBy(() -> recordService.seal(userId, draft.getId()))
                .isInstanceOf(BizException.class)
                .hasMessage("仅SAVED状态允许封存");

        assertThat(recordMapper.selectByIdAndUserId(draft.getId(), userId).getStatus())
                .isEqualTo(RecordStatus.DRAFT);
    }

    @Test
    void shouldSealOnlyAfterMomentHasBeenSaved() {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent("先让这一刻成立");
        request.setUnlockAt(LocalDateTime.now().plusDays(30));
        var draft = recordService.create(userId, request);
        recordService.save(userId, draft.getId());

        var sealed = recordService.seal(userId, draft.getId());

        assertThat(sealed.getStatus()).isEqualTo(RecordStatus.SEALED);
        assertThat(sealed.getSealedAt()).isNotNull();
    }

    @Test
    void shouldEditSavedTextWithoutChangingSavedState() {
        CreateRecordRequest create = new CreateRecordRequest();
        create.setContent("原来的一句话");
        var draft = recordService.create(userId, create);
        recordService.save(userId, draft.getId());

        UpdateRecordRequest update = new UpdateRecordRequest();
        update.setContent("后来想补充的一句话");
        update.setRecordType(RecordType.MOMENT);
        var updated = recordService.update(userId, draft.getId(), update);

        assertThat(updated.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(updated.getContent()).isEqualTo("后来想补充的一句话");
    }

    @Test
    void shouldRejectClearingLastSavedEvidence() {
        CreateRecordRequest create = new CreateRecordRequest();
        create.setContent("唯一的一句话");
        var draft = recordService.create(userId, create);
        recordService.save(userId, draft.getId());

        UpdateRecordRequest update = new UpdateRecordRequest();
        update.setContent("   ");
        update.setRecordType(RecordType.MOMENT);

        assertThatThrownBy(() -> recordService.update(userId, draft.getId(), update))
                .isInstanceOf(BizException.class)
                .hasMessage("至少留下一句话、一张图片或一段声音");
        assertThat(recordMapper.selectByIdAndUserId(draft.getId(), userId).getContent())
                .isEqualTo("唯一的一句话");
    }

    @Test
    void shouldAllowClearingSavedTextWhenImageRemains() {
        CreateRecordRequest create = new CreateRecordRequest();
        create.setContent("稍后可以清空的正文");
        var draft = recordService.create(userId, create);
        insertAttachment(draft.getId(), "IMAGE", "AVAILABLE", "still-here.jpg");
        recordService.save(userId, draft.getId());

        UpdateRecordRequest update = new UpdateRecordRequest();
        update.setContent("");
        update.setRecordType(RecordType.MOMENT);
        var updated = recordService.update(userId, draft.getId(), update);

        assertThat(updated.getContent()).isEmpty();
        assertThat(updated.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(updated.getAttachments()).hasSize(1);
    }

    @Test
    void shouldHideDraftByDefaultAndExposeOnlyActiveDraftRecovery() {
        var draft = recordService.create(userId, new CreateRecordRequest());

        assertThat(recordMapper.countByUserAndCondition(userId, null, null, null, null)).isZero();
        assertThat(recordMapper.countByUserAndCondition(userId, RecordStatus.DRAFT, null, null, null)).isOne();

        jdbcTemplate.update(
                "UPDATE `record` SET draft_expires_at = ? WHERE id = ?",
                LocalDateTime.of(2000, 1, 1, 0, 0),
                draft.getId());
        sqlSessionTemplate.clearCache();

        assertThat(recordMapper.countByUserAndCondition(userId, RecordStatus.DRAFT, null, null, null)).isZero();
    }

    @Test
    void shouldDeleteOnlyTheSameExpiredDraftVersion() {
        var draft = recordService.create(userId, new CreateRecordRequest());
        LocalDateTime expiry = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 0, 0);
        jdbcTemplate.update("UPDATE `record` SET draft_expires_at = ? WHERE id = ?", expiry, draft.getId());

        assertThat(recordMapper.selectExpiredDrafts(now, 50))
                .extracting(Record::getId)
                .contains(draft.getId());
        assertThat(recordMapper.selectExpiredDraftForUpdate(draft.getId(), userId, expiry, now)).isNotNull();
        assertThat(recordMapper.deleteExpiredDraftByIdAndUserId(draft.getId(), userId, expiry, now)).isOne();
        assertThat(recordMapper.selectByIdAndUserId(draft.getId(), userId)).isNull();
    }

    private void insertAttachment(Long recordId, String type, String status, String fileName) {
        jdbcTemplate.update(
                """
                        INSERT INTO record_attachment (
                            record_id, user_id, type, storage_provider, bucket, storage_key,
                            file_name, mime_type, size_bytes, sort_order, status, created_at, updated_at
                        ) VALUES (?, ?, ?, 'QINIU', 'test-private', ?, ?, ?, 1024, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                recordId,
                userId,
                type,
                "flashback/test/" + recordId + "/" + fileName,
                fileName,
                "IMAGE".equals(type) ? "image/jpeg" : "audio/mpeg",
                status);
    }
}
