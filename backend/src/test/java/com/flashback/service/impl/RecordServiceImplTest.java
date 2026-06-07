package com.flashback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.LifeNodeType;
import com.flashback.domain.Record;
import com.flashback.domain.RecordReminder;
import com.flashback.domain.RecordReminderStatus;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.User;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.mapper.RecordReminderMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.UnlockNoticeLogMapper;
import com.flashback.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordServiceImplTest {

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private UnlockNoticeLogMapper unlockNoticeLogMapper;

    @Mock
    private RecordReminderMapper recordReminderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private RecordTagMapper recordTagMapper;

    @Mock
    private ReplyMapper replyMapper;

    private RecordServiceImpl recordService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-26T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
        recordService = new RecordServiceImpl(
                recordMapper,
                tagMapper,
                recordTagMapper,
                replyMapper,
                unlockNoticeLogMapper,
                recordReminderMapper,
                userMapper,
                new ObjectMapper(),
                clock);
    }

    @Test
    void shouldRejectUpdateWhenRecordNotOwned() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(null);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);

        assertThatThrownBy(() -> recordService.update(1L, 100L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
    }

    @Test
    void shouldRejectUpdateWhenStatusIsNotDraft() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);

        assertThatThrownBy(() -> recordService.update(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("仅DRAFT状态允许编辑");
    }

    @Test
    void shouldRejectDeleteWhenStatusIsNotDraft() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(101L, 1L)).thenReturn(sealed);

        assertThatThrownBy(() -> recordService.delete(1L, 101L))
                .isInstanceOf(BizException.class)
                .hasMessage("仅DRAFT状态允许删除");
    }

    @Test
    void shouldClearRecordTagsWhenDeleteDraft() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(101L, 1L)).thenReturn(draft);
        when(recordMapper.deleteDraftByIdAndUserId(101L, 1L)).thenReturn(1);

        recordService.delete(1L, 101L);

        verify(recordTagMapper).deleteByRecordId(101L);
    }

    @Test
    void shouldRejectSealWhenUnlockAtBeforeNow() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        draft.setUnlockAt(LocalDateTime.of(2026, 3, 26, 15, 30, 0));
        when(recordMapper.selectByIdAndUserId(102L, 1L)).thenReturn(draft);

        assertThatThrownBy(() -> recordService.seal(1L, 102L))
                .isInstanceOf(BizException.class)
                .hasMessage("unlockAt必须晚于当前时间");
    }

    @Test
    void shouldReturnCorrectPageStructureForMineList() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.countByUserAndCondition(1L, RecordStatus.DRAFT, RecordType.NODE_RECORD, null, null))
                .thenReturn(1L);
        when(recordMapper.selectPageByUserAndCondition(1L, RecordStatus.DRAFT, RecordType.NODE_RECORD, null, null, 0,
                10))
                .thenReturn(List.of(draft));

        RecordPageQuery query = new RecordPageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setStatus(RecordStatus.DRAFT);
        query.setRecordType(RecordType.NODE_RECORD);

        var result = recordService.pageMine(1L, query);
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getStatus()).isEqualTo(RecordStatus.DRAFT);
    }

    @Test
    void shouldRejectUpdateWhenTagNotExists() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);
        when(tagMapper.countEnabledByIds(List.of(1L, 2L))).thenReturn(1L);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setTagIds(List.of(1L, 2L));

        assertThatThrownBy(() -> recordService.update(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("标签不存在");
    }

    @Test
    void shouldRebindTagsWhenUpdateDraft() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, draft);
        when(tagMapper.countEnabledByIds(List.of(1L, 2L))).thenReturn(2L);
        when(recordMapper.updateDraftByIdAndUserId(eq(100L), eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setTitle("updated");
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setTagIds(List.of(1L, 2L, 2L));
        request.setAiSummary("新的AI总结");
        request.setAiPromptResults(List.of("先投递", "先投递", "补项目"));
        request.setBeliefThen("当时以为只要多投递就会好");
        request.setLifeNodeType(LifeNodeType.OTHER);
        request.setLifeNodeCustomLabel("转专业");

        recordService.update(1L, 100L, request);

        verify(recordTagMapper, times(1)).deleteByRecordId(100L);
        verify(recordTagMapper, times(1)).batchInsert(100L, List.of(1L, 2L));
    }

    @Test
    void shouldPersistAiFieldsWhenCreateDraft() {
        when(recordMapper.insert(any(Record.class))).thenAnswer(invocation -> {
            Record record = invocation.getArgument(0);
            record.setId(501L);
            return 1;
        });
        when(recordMapper.selectByIdAndUserId(501L, 1L)).thenAnswer(invocation -> {
            Record record = mockRecord(RecordStatus.DRAFT);
            record.setId(501L);
            return record;
        });

        com.flashback.dto.CreateRecordRequest request = new com.flashback.dto.CreateRecordRequest();
        request.setContent("今天先把问题写下来");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setAiSummary("当前主要困惑在求职节奏");
        request.setAiPromptResults(List.of("你最担心什么？", "今天先做哪一步？"));

        recordService.create(1L, request);

        verify(recordMapper).insert(org.mockito.ArgumentMatchers.argThat(record ->
                "当前主要困惑在求职节奏".equals(record.getAiSummary())
                        && "[\"你最担心什么？\",\"今天先做哪一步？\"]".equals(record.getAiPromptResult())));
    }

    @Test
    void shouldCreateDraftWithoutAiSnapshotAndPreserveOriginalContent() {
        when(recordMapper.insert(any(Record.class))).thenAnswer(invocation -> {
            Record record = invocation.getArgument(0);
            record.setId(502L);
            return 1;
        });
        when(recordMapper.selectByIdAndUserId(502L, 1L)).thenAnswer(invocation -> {
            Record record = mockRecord(RecordStatus.DRAFT);
            record.setId(502L);
            record.setContent("不使用 AI，也先把今天的真实想法写下来");
            record.setAiSummary(null);
            record.setAiPromptResult(null);
            return record;
        });

        com.flashback.dto.CreateRecordRequest request = new com.flashback.dto.CreateRecordRequest();
        request.setTitle("无 AI 草稿");
        request.setContent("不使用 AI，也先把今天的真实想法写下来");
        request.setRecordType(RecordType.EMOTION_NOTE);

        var result = recordService.create(1L, request);

        verify(recordMapper).insert(org.mockito.ArgumentMatchers.argThat(record ->
                "不使用 AI，也先把今天的真实想法写下来".equals(record.getContent())
                        && record.getAiSummary() == null
                        && record.getAiPromptResult() == null
                        && record.getStatus() == RecordStatus.DRAFT));
        assertThat(result.getContent()).isEqualTo("不使用 AI，也先把今天的真实想法写下来");
        assertThat(result.getAiSummary()).isNull();
        assertThat(result.getAiPromptResults()).isEmpty();
    }

    @Test
    void shouldClearAiFieldsWhenUpdateDraftWithoutAiSnapshot() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, draft);
        when(recordMapper.updateDraftByIdAndUserId(eq(100L), eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setTitle("updated");
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setAiSummary(null);
        request.setAiPromptResults(List.of());

        recordService.update(1L, 100L, request);

        verify(recordMapper).updateDraftByIdAndUserId(
                eq(100L),
                eq(1L),
                any(),
                any(),
                any(),
                any(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                any());
    }

    @Test
    void shouldRejectCustomLifeNodeLabelWhenTypeIsNotOther() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setLifeNodeType(LifeNodeType.WORK);
        request.setLifeNodeCustomLabel("自定义工作节点");

        assertThatThrownBy(() -> recordService.update(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("lifeNodeCustomLabel仅在lifeNodeType为OTHER时允许填写");
    }

    @Test
    void shouldGroupTimelineByYearMonth() {
        Record march = mockRecord(RecordStatus.SEALED);
        march.setId(201L);
        march.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));

        Record february = mockRecord(RecordStatus.UNLOCKED);
        february.setId(202L);
        february.setCreatedAt(LocalDateTime.of(2026, 2, 10, 11, 0, 0));

        when(recordMapper.selectTimelineByUserAndCondition(1L, null, 2026)).thenReturn(List.of(march, february));

        com.flashback.domain.RecordTagName row = new com.flashback.domain.RecordTagName();
        row.setRecordId(201L);
        row.setTagName("焦虑");
        when(tagMapper.selectTagNamesByRecordIds(List.of(201L, 202L))).thenReturn(new ArrayList<>(List.of(row)));

        RecordTimelineQuery query = new RecordTimelineQuery();
        query.setYear(2026);

        var timeline = recordService.timeline(1L, query);
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).getYearMonth()).isEqualTo("2026-03");
        assertThat(timeline.get(0).getItems()).hasSize(1);
        assertThat(timeline.get(0).getItems().get(0).getTagNames()).containsExactly("焦虑");
        assertThat(timeline.get(1).getYearMonth()).isEqualTo("2026-02");
    }

    @Test
    void shouldSealDraftSuccessfully() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        draft.setUnlockAt(LocalDateTime.of(2026, 3, 27, 10, 0, 0));

        when(recordMapper.selectByIdAndUserId(103L, 1L)).thenReturn(draft, sealedRecord());
        when(recordMapper.sealDraftByIdAndUserId(eq(103L), eq(1L), any(), any())).thenReturn(1);

        var result = recordService.seal(1L, 103L);
        assertThat(result.getStatus()).isEqualTo(RecordStatus.SEALED);
        assertThat(result.getSealedAt()).isNotNull();
    }

    @Test
    void shouldReturnDetailWithAiFieldsAndReplyFlags() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        unlocked.setAiSummary("当前的困惑集中在实习准备");
        unlocked.setAiPromptResult("[\"你最担心的是什么？\",\"下一步先做哪件事？\"]");
        unlocked.setBeliefThen("那时以为只要准备充分就不会紧张");
        unlocked.setLifeNodeType(LifeNodeType.WORK);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);
        when(replyMapper.selectByRecordId(100L)).thenReturn(mockReply(100L, 1L, "已写回信"));

        var result = recordService.detail(1L, 100L);

        assertThat(result.getAiSummary()).isEqualTo("当前的困惑集中在实习准备");
        assertThat(result.getAiPromptResults()).containsExactly("你最担心的是什么？", "下一步先做哪件事？");
        assertThat(result.getBeliefThen()).isEqualTo("那时以为只要准备充分就不会紧张");
        assertThat(result.getLifeNodeType()).isEqualTo(LifeNodeType.WORK);
        assertThat(result.getHasReply()).isTrue();
        assertThat(result.getCanReply()).isFalse();
    }

    @Test
    void shouldAllowReplyWhenUnlockedRecordHasNoReply() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        var result = recordService.detail(1L, 100L);

        assertThat(result.getHasReply()).isFalse();
        assertThat(result.getCanReply()).isTrue();
        assertThat(result.getAiPromptResults()).containsExactly("你最担心的是什么？", "下一步先做哪件事？");
    }

    @Test
    void shouldFallbackToSinglePromptWhenStoredAiPromptIsLegacyPlainText() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        unlocked.setAiPromptResult("旧版单条提示");
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        var result = recordService.detail(1L, 100L);

        assertThat(result.getAiPromptResults()).containsExactly("旧版单条提示");
    }

    @Test
    void shouldUnlockExpiredSealedRecordsAndWriteLog() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(201L);
        expired.setUserId(11L);
        expired.setUnlockAt(LocalDateTime.of(2026, 3, 26, 15, 0, 0));

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(201L), any(), any())).thenReturn(1);
        when(userMapper.selectById(11L)).thenReturn(mockUser(11L, null));

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordMapper, times(1)).unlockSealedById(eq(201L), any(), any());
        verify(unlockNoticeLogMapper, times(1)).insert(any());
        verify(recordReminderMapper, times(1)).insert(org.mockito.ArgumentMatchers.argThat(reminder ->
                reminder.getRecordId().equals(201L)
                        && reminder.getUserId().equals(11L)
                        && "UNLOCK_REMINDER".equals(reminder.getTemplateType())
                        && reminder.getReminderStatus() == RecordReminderStatus.SKIPPED_NO_OPENID
                        && "openid not bound".equals(reminder.getLastError())));
    }

    @Test
    void shouldNotUnlockWhenRecordNotExpired() {
        when(recordMapper.selectExpiredSealedRecords(any(), eq(100))).thenReturn(List.of());

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(0);
        verify(recordMapper, never()).unlockSealedById(any(), any(), any());
        verify(unlockNoticeLogMapper, never()).insert(any());
        verify(recordReminderMapper, never()).insert(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyUnlockedByAnotherRun() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(301L);
        expired.setUserId(21L);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(301L), any(), any())).thenReturn(0);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(0);
        verify(unlockNoticeLogMapper, never()).insert(any());
        verify(recordReminderMapper, never()).insert(any());
    }

    @Test
    void shouldCreatePendingUnlockReminderWhenUserHasOpenid() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(401L);
        expired.setUserId(31L);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(401L), any(), any())).thenReturn(1);
        when(userMapper.selectById(31L)).thenReturn(mockUser(31L, "openid-31"));

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordReminderMapper).insert(org.mockito.ArgumentMatchers.argThat(reminder ->
                reminder.getRecordId().equals(401L)
                        && reminder.getUserId().equals(31L)
                        && "UNLOCK_REMINDER".equals(reminder.getTemplateType())
                        && reminder.getReminderStatus() == RecordReminderStatus.PENDING
                        && reminder.getLastError() == null));
    }

    @Test
    void shouldNotCreateDuplicateUnlockReminderWhenMarkerExists() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(402L);
        expired.setUserId(32L);

        RecordReminder existing = new RecordReminder();
        existing.setRecordId(402L);
        existing.setTemplateType("UNLOCK_REMINDER");

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(402L), any(), any())).thenReturn(1);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(402L, "UNLOCK_REMINDER")).thenReturn(existing);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordReminderMapper, never()).insert(any());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void shouldContinueUnlockWhenReminderPersistenceFails() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(403L);
        expired.setUserId(33L);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(403L), any(), any())).thenReturn(1);
        doThrow(new RuntimeException("record_reminder unavailable"))
                .when(recordReminderMapper)
                .selectByRecordIdAndTemplateType(403L, "UNLOCK_REMINDER");

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(unlockNoticeLogMapper).insert(any());
    }

    private Record mockRecord(RecordStatus status) {
        Record record = new Record();
        record.setId(100L);
        record.setUserId(1L);
        record.setTitle("节点记录");
        record.setContent("今天写下阶段总结");
        record.setRecordType(RecordType.NODE_RECORD);
        record.setCoreQuestion("下一步怎么走");
        record.setStatus(status);
        record.setAiSummary("AI总结");
        record.setAiPromptResult("[\"你最担心的是什么？\",\"下一步先做哪件事？\"]");
        record.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
        return record;
    }

    private Record sealedRecord() {
        Record record = mockRecord(RecordStatus.SEALED);
        record.setSealedAt(LocalDateTime.of(2026, 3, 26, 16, 0, 0));
        return record;
    }

    private com.flashback.domain.Reply mockReply(Long recordId, Long userId, String content) {
        com.flashback.domain.Reply reply = new com.flashback.domain.Reply();
        reply.setId(200L);
        reply.setRecordId(recordId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setCreatedAt(LocalDateTime.of(2026, 3, 26, 18, 0, 0));
        return reply;
    }

    private User mockUser(Long userId, String openid) {
        User user = new User();
        user.setId(userId);
        user.setOpenid(openid);
        return user;
    }
}
