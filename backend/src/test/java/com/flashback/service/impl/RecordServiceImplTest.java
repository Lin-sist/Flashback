package com.flashback.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppWechatProperties;
import com.flashback.domain.LifeNodeType;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordLocation;
import com.flashback.domain.RecordLocationSource;
import com.flashback.domain.RecordReminder;
import com.flashback.domain.RecordReminderStatus;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.User;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateLaterReflectionRequest;
import com.flashback.dto.UpdateRecordCoverRequest;
import com.flashback.dto.UpdateRecordLocationRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.dto.UpdateUnlockReminderAuthorizationRequest;
import com.flashback.mapper.RecordLocationMapper;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordReminderMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.UnlockNoticeLogMapper;
import com.flashback.mapper.UserMapper;
import com.flashback.wechat.WechatSubscribeMessageClient;
import com.flashback.service.RecordSaveEligibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    private RecordLocationMapper recordLocationMapper;

    @Mock
    private RecordAttachmentMapper recordAttachmentMapper;

    @Mock
    private ReplyMapper replyMapper;

    @Mock
    private WechatSubscribeMessageClient wechatSubscribeMessageClient;

    private AppWechatProperties appWechatProperties;

    private RecordServiceImpl recordService;

    @Mock
    private RecordSaveEligibility recordSaveEligibility;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-26T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
        appWechatProperties = new AppWechatProperties();
        recordService = new RecordServiceImpl(
                recordMapper,
                tagMapper,
                recordTagMapper,
                recordLocationMapper,
                recordAttachmentMapper,
                replyMapper,
                unlockNoticeLogMapper,
                recordReminderMapper,
                userMapper,
                new ObjectMapper(),
                appWechatProperties,
                wechatSubscribeMessageClient,
                clock,
                recordSaveEligibility);
        lenient().when(recordMapper.touchDraftByIdAndUserId(any(), any(), any(), any())).thenReturn(1);
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
                .hasMessage("仅DRAFT或SAVED状态允许编辑");
    }

    @Test
    void shouldMigrateLegacyDeleteEndpointToOwnershipFlow() {
        assertThatThrownBy(() -> recordService.delete(1L, 101L))
                .isInstanceOf(BizException.class)
                .hasMessage("请通过数据与所有权的二次确认流程删除记录");
        verify(recordMapper, never()).deleteDraftByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldNotUseLegacyDirectDeleteForDraft() {
        assertThatThrownBy(() -> recordService.delete(1L, 101L)).isInstanceOf(BizException.class);
        verify(recordTagMapper, never()).deleteByRecordId(anyLong());
    }

    @Test
    void shouldSaveManualLocationForDraftRecord() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        RecordLocation saved = new RecordLocation();
        saved.setSource(RecordLocationSource.MANUAL);
        saved.setName("人民公园");
        saved.setAddress("上海市黄浦区南京西路");
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, draft);
        when(recordLocationMapper.selectByRecordIdAndUserId(100L, 1L)).thenReturn(saved);

        UpdateRecordLocationRequest request = new UpdateRecordLocationRequest();
        request.setSource(RecordLocationSource.MANUAL);
        request.setName(" 人民公园 ");
        request.setAddress(" 上海市黄浦区南京西路 ");

        var result = recordService.updateLocation(1L, 100L, request);

        verify(recordLocationMapper).upsert(org.mockito.ArgumentMatchers.argThat(location ->
                location.getRecordId().equals(100L)
                        && location.getUserId().equals(1L)
                        && location.getSource() == RecordLocationSource.MANUAL
                        && "人民公园".equals(location.getName())
                        && "上海市黄浦区南京西路".equals(location.getAddress())));
        assertThat(result.getLocation()).isNotNull();
        assertThat(result.getLocation().getSource()).isEqualTo(RecordLocationSource.MANUAL);
        assertThat(result.getLocation().getName()).isEqualTo("人民公园");
    }

    @Test
    void shouldRejectMapPickerLocationWithoutCoordinates() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);

        UpdateRecordLocationRequest request = new UpdateRecordLocationRequest();
        request.setSource(RecordLocationSource.MAP_PICKER);
        request.setName("人民公园");

        assertThatThrownBy(() -> recordService.updateLocation(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("CURRENT_LOCATION和MAP_PICKER位置必须包含latitude和longitude");
        verify(recordLocationMapper, never()).upsert(any());
    }

    @Test
    void shouldRejectManualLocationWithoutNameOrAddress() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);

        UpdateRecordLocationRequest request = new UpdateRecordLocationRequest();
        request.setSource(RecordLocationSource.MANUAL);

        assertThatThrownBy(() -> recordService.updateLocation(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("MANUAL位置必须填写name或address");
        verify(recordLocationMapper, never()).upsert(any());
    }

    @Test
    void shouldRejectLocationMutationWhenRecordIsSealed() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed);

        UpdateRecordLocationRequest request = new UpdateRecordLocationRequest();
        request.setSource(RecordLocationSource.CURRENT_LOCATION);
        request.setLatitude(BigDecimal.valueOf(31.2317));
        request.setLongitude(BigDecimal.valueOf(121.4746));

        assertThatThrownBy(() -> recordService.updateLocation(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("记录已封存，不能编辑位置");
        verify(recordLocationMapper, never()).upsert(any());
    }

    @Test
    void shouldDeleteLocationForDraftRecord() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, draft);

        recordService.deleteLocation(1L, 100L);

        verify(recordLocationMapper).deleteByRecordIdAndUserId(100L, 1L);
    }

    @Test
    void shouldSetImageAttachmentAsCoverForDraftRecord() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        Record updated = mockRecord(RecordStatus.DRAFT);
        updated.setCoverAttachmentId(77L);
        RecordAttachment cover = attachment(77L, RecordAttachmentType.IMAGE);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, updated);
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(77L, 100L, 1L)).thenReturn(cover, cover);
        when(recordMapper.updateCoverAttachmentByIdAndUserId(
                eq(100L),
                eq(1L),
                eq(77L),
                any())).thenReturn(1);

        UpdateRecordCoverRequest request = new UpdateRecordCoverRequest();
        request.setAttachmentId(77L);

        var result = recordService.updateCover(1L, 100L, request);

        verify(recordMapper).updateCoverAttachmentByIdAndUserId(eq(100L), eq(1L), eq(77L), any());
        assertThat(result.getCover()).isNotNull();
        assertThat(result.getCover().getId()).isEqualTo(77L);
        assertThat(result.getCover().getType()).isEqualTo(RecordAttachmentType.IMAGE);
    }

    @Test
    void shouldClearCoverForDraftRecord() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        Record updated = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft, updated);
        when(recordMapper.updateCoverAttachmentByIdAndUserId(
                eq(100L),
                eq(1L),
                org.mockito.ArgumentMatchers.isNull(),
                any())).thenReturn(1);

        UpdateRecordCoverRequest request = new UpdateRecordCoverRequest();

        var result = recordService.updateCover(1L, 100L, request);

        verify(recordMapper).updateCoverAttachmentByIdAndUserId(
                eq(100L),
                eq(1L),
                org.mockito.ArgumentMatchers.isNull(),
                any());
        assertThat(result.getCover()).isNull();
    }

    @Test
    void shouldRejectVoiceAttachmentAsCover() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);
        when(recordAttachmentMapper.selectByIdAndRecordIdAndUserId(88L, 100L, 1L))
                .thenReturn(attachment(88L, RecordAttachmentType.VOICE));

        UpdateRecordCoverRequest request = new UpdateRecordCoverRequest();
        request.setAttachmentId(88L);

        assertThatThrownBy(() -> recordService.updateCover(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("封面必须选择图片附件");
        verify(recordMapper, never()).updateCoverAttachmentByIdAndUserId(any(), any(), any(), any());
    }

    @Test
    void shouldRejectCoverMutationWhenRecordIsSealed() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed);

        UpdateRecordCoverRequest request = new UpdateRecordCoverRequest();
        request.setAttachmentId(77L);

        assertThatThrownBy(() -> recordService.updateCover(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("记录已封存，不能设置封面");
        verify(recordAttachmentMapper, never()).selectByIdAndRecordIdAndUserId(any(), any(), any());
    }

    @Test
    void shouldRejectSealWhenUnlockAtBeforeNow() {
        Record saved = mockRecord(RecordStatus.SAVED);
        saved.setUnlockAt(LocalDateTime.of(2026, 3, 26, 15, 30, 0));
        when(recordMapper.selectByIdAndUserId(102L, 1L)).thenReturn(saved);

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
        when(recordMapper.updateEditableByIdAndUserId(eq(100L), eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
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
        when(recordMapper.updateEditableByIdAndUserId(eq(100L), eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        UpdateRecordRequest request = new UpdateRecordRequest();
        request.setTitle("updated");
        request.setContent("new content");
        request.setRecordType(RecordType.NODE_RECORD);
        request.setAiSummary(null);
        request.setAiPromptResults(List.of());

        recordService.update(1L, 100L, request);

        verify(recordMapper).updateEditableByIdAndUserId(
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
    void shouldRejectLaterReflectionBeforeUnlock() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed);

        UpdateLaterReflectionRequest request = new UpdateLaterReflectionRequest();
        request.setRealityLater("后来其实");

        assertThatThrownBy(() -> recordService.updateLaterReflection(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("仅UNLOCKED状态允许填写后来其实");
        verify(recordMapper, never()).updateLaterReflectionByIdAndUserId(any(), any(), any(), any());
    }

    @Test
    void shouldRejectLaterReflectionWhenRecordNotOwned() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(null);

        UpdateLaterReflectionRequest request = new UpdateLaterReflectionRequest();
        request.setRealityLater("后来其实");

        assertThatThrownBy(() -> recordService.updateLaterReflection(1L, 100L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
        verify(recordMapper, never()).updateLaterReflectionByIdAndUserId(any(), any(), any(), any());
    }

    @Test
    void shouldUpdateLaterReflectionAfterUnlockWithinLimit() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        unlocked.setRealityLaterSubmitCount(1);

        Record updated = mockRecord(RecordStatus.UNLOCKED);
        updated.setRealityLater("后来其实我需要重新理解当时的焦虑");
        updated.setRealityLaterSubmitCount(2);

        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked, updated);
        when(recordMapper.updateLaterReflectionByIdAndUserId(eq(100L), eq(1L), any(), any())).thenReturn(1);
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        UpdateLaterReflectionRequest request = new UpdateLaterReflectionRequest();
        request.setRealityLater("后来其实我需要重新理解当时的焦虑");

        var result = recordService.updateLaterReflection(1L, 100L, request);

        assertThat(result.getRealityLater()).isEqualTo("后来其实我需要重新理解当时的焦虑");
        verify(recordMapper).updateLaterReflectionByIdAndUserId(
                eq(100L),
                eq(1L),
                eq("后来其实我需要重新理解当时的焦虑"),
                any());
    }

    @Test
    void shouldRejectLaterReflectionWhenSubmitLimitExhausted() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        unlocked.setRealityLaterSubmitCount(2);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);

        UpdateLaterReflectionRequest request = new UpdateLaterReflectionRequest();
        request.setRealityLater("第三次修改");

        assertThatThrownBy(() -> recordService.updateLaterReflection(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("后来其实提交次数已用完");
        verify(recordMapper, never()).updateLaterReflectionByIdAndUserId(any(), any(), any(), any());
    }

    @Test
    void shouldGroupTimelineByYearMonth() {
        Record march = mockRecord(RecordStatus.SEALED);
        march.setId(201L);
        march.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));

        Record february = mockRecord(RecordStatus.UNLOCKED);
        february.setId(202L);
        february.setCreatedAt(LocalDateTime.of(2026, 2, 10, 11, 0, 0));

        LocalDateTime createdFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime createdBefore = LocalDateTime.of(2027, 1, 1, 0, 0);
        when(recordMapper.countTimelineByUserAndCondition(1L, null, createdFrom, createdBefore)).thenReturn(2L);
        when(recordMapper.selectTimelinePageByUserAndCondition(
                1L,
                null,
                createdFrom,
                createdBefore,
                0,
                20)).thenReturn(List.of(march, february));

        com.flashback.domain.RecordTagName row = new com.flashback.domain.RecordTagName();
        row.setRecordId(201L);
        row.setTagName("焦虑");
        when(tagMapper.selectTagNamesByRecordIds(List.of(201L, 202L))).thenReturn(new ArrayList<>(List.of(row)));

        RecordTimelineQuery query = new RecordTimelineQuery();
        query.setYear(2026);

        var timeline = recordService.timeline(1L, query);
        assertThat(timeline.getTotal()).isEqualTo(2L);
        assertThat(timeline.getPageNum()).isEqualTo(1);
        assertThat(timeline.getPageSize()).isEqualTo(20);
        assertThat(timeline.isHasMore()).isFalse();
        assertThat(timeline.getGroups()).hasSize(2);
        assertThat(timeline.getGroups().get(0).getYearMonth()).isEqualTo("2026-03");
        assertThat(timeline.getGroups().get(0).getItems()).hasSize(1);
        assertThat(timeline.getGroups().get(0).getItems().get(0).getTagNames()).containsExactly("焦虑");
        assertThat(timeline.getGroups().get(1).getYearMonth()).isEqualTo("2026-02");
    }

    @Test
    void shouldResolveExactDayRangeAndHasMore() {
        LocalDateTime createdFrom = LocalDateTime.of(2026, 6, 22, 0, 0);
        LocalDateTime createdBefore = LocalDateTime.of(2026, 6, 23, 0, 0);
        when(recordMapper.countTimelineByUserAndCondition(1L, 12L, createdFrom, createdBefore)).thenReturn(21L);
        when(recordMapper.selectTimelinePageByUserAndCondition(
                1L,
                12L,
                createdFrom,
                createdBefore,
                0,
                20)).thenReturn(List.of());

        RecordTimelineQuery query = new RecordTimelineQuery();
        query.setTagId(12L);
        query.setYear(2026);
        query.setMonth(6);
        query.setDay(22);

        var timeline = recordService.timeline(1L, query);

        assertThat(timeline.getTotal()).isEqualTo(21L);
        assertThat(timeline.isHasMore()).isTrue();
        assertThat(timeline.getGroups()).isEmpty();
    }

    @Test
    void shouldSkipTimelinePageQueryWhenNoRecordsMatch() {
        when(recordMapper.countTimelineByUserAndCondition(1L, 999L, null, null)).thenReturn(0L);

        RecordTimelineQuery query = new RecordTimelineQuery();
        query.setTagId(999L);

        var timeline = recordService.timeline(1L, query);

        assertThat(timeline.getTotal()).isZero();
        assertThat(timeline.getGroups()).isEmpty();
        verify(recordMapper, never()).selectTimelinePageByUserAndCondition(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldSealSavedSuccessfully() {
        Record saved = mockRecord(RecordStatus.SAVED);
        saved.setUnlockAt(LocalDateTime.of(2026, 3, 27, 10, 0, 0));

        when(recordMapper.selectByIdAndUserId(103L, 1L)).thenReturn(saved, sealedRecord());
        when(recordMapper.sealSavedByIdAndUserId(eq(103L), eq(1L), any(), any())).thenReturn(1);

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
        unlocked.setRealityLaterSubmitCount(1);
        unlocked.setLifeNodeType(LifeNodeType.WORK);
        RecordReminder reminder = new RecordReminder();
        reminder.setReminderStatus(RecordReminderStatus.SEND_SUCCESS);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);
        when(replyMapper.selectByRecordId(100L)).thenReturn(mockReply(100L, 1L, "已写回信"));
        when(recordReminderMapper.selectByRecordIdAndTemplateType(100L, "UNLOCK_REMINDER")).thenReturn(reminder);

        var result = recordService.detail(1L, 100L);

        assertThat(result.getAiSummary()).isEqualTo("当前的困惑集中在实习准备");
        assertThat(result.getAiPromptResults()).containsExactly("你最担心的是什么？", "下一步先做哪件事？");
        assertThat(result.getBeliefThen()).isEqualTo("那时以为只要准备充分就不会紧张");
        assertThat(result.getRealityLaterSubmitCount()).isEqualTo(1);
        assertThat(result.getLifeNodeType()).isEqualTo(LifeNodeType.WORK);
        assertThat(result.getLifeNodeLabel()).isEqualTo("工作");
        assertThat(result.getUnlockReminderStatus()).isEqualTo(RecordReminderStatus.SEND_SUCCESS);
        assertThat(result.getHasReply()).isTrue();
        assertThat(result.getCanReply()).isFalse();
    }

    @Test
    void shouldCreateUnlockReminderAuthorizationResult() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        RecordReminder created = new RecordReminder();
        created.setReminderStatus(RecordReminderStatus.DENIED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed, sealed);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(100L, "UNLOCK_REMINDER"))
                .thenReturn(null)
                .thenReturn(created);

        UpdateUnlockReminderAuthorizationRequest request = new UpdateUnlockReminderAuthorizationRequest();
        request.setStatus(RecordReminderStatus.DENIED);

        var result = recordService.updateUnlockReminderAuthorization(1L, 100L, request);

        verify(recordReminderMapper).insert(org.mockito.ArgumentMatchers.argThat(reminder ->
                reminder.getRecordId().equals(100L)
                        && reminder.getUserId().equals(1L)
                        && "UNLOCK_REMINDER".equals(reminder.getTemplateType())
                        && reminder.getReminderStatus() == RecordReminderStatus.DENIED
                        && "wechat subscription authorization denied".equals(reminder.getLastError())));
        assertThat(result.getUnlockReminderStatus()).isEqualTo(RecordReminderStatus.DENIED);
    }

    @Test
    void shouldRejectUnsupportedUnlockReminderAuthorizationStatus() {
        Record sealed = mockRecord(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(sealed);

        UpdateUnlockReminderAuthorizationRequest request = new UpdateUnlockReminderAuthorizationRequest();
        request.setStatus(RecordReminderStatus.SEND_SUCCESS);

        assertThatThrownBy(() -> recordService.updateUnlockReminderAuthorization(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("提醒授权状态仅支持REQUESTED、AUTHORIZED或DENIED");
        verify(recordReminderMapper, never()).insert(any());
        verify(recordReminderMapper, never()).updateStatusById(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectUnlockReminderAuthorizationForDraft() {
        Record draft = mockRecord(RecordStatus.DRAFT);
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(draft);

        UpdateUnlockReminderAuthorizationRequest request = new UpdateUnlockReminderAuthorizationRequest();
        request.setStatus(RecordReminderStatus.AUTHORIZED);

        assertThatThrownBy(() -> recordService.updateUnlockReminderAuthorization(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("仅封存后的记录允许更新提醒授权状态");
        verify(recordReminderMapper, never()).insert(any());
    }

    @Test
    void shouldReturnCustomLifeNodeLabelForOther() {
        Record unlocked = mockRecord(RecordStatus.UNLOCKED);
        unlocked.setLifeNodeType(LifeNodeType.OTHER);
        unlocked.setLifeNodeCustomLabel("换城市");
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(unlocked);
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        var result = recordService.detail(1L, 100L);

        assertThat(result.getLifeNodeType()).isEqualTo(LifeNodeType.OTHER);
        assertThat(result.getLifeNodeCustomLabel()).isEqualTo("换城市");
        assertThat(result.getLifeNodeLabel()).isEqualTo("换城市");
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
    void shouldCreateNotConfiguredUnlockReminderWhenUserHasOpenidButTemplateMissing() {
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
                        && reminder.getReminderStatus() == RecordReminderStatus.NOT_CONFIGURED
                        && "wechat unlock reminder template not configured".equals(reminder.getLastError())));
    }

    @Test
    void shouldRecordRequestedWhenTemplateConfiguredButAuthorizationMissing() {
        appWechatProperties.setUnlockReminderTemplateId("template-id");

        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(404L);
        expired.setUserId(34L);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(404L), any(), any())).thenReturn(1);
        when(userMapper.selectById(34L)).thenReturn(mockUser(34L, "openid-34"));
        when(recordReminderMapper.insert(any(RecordReminder.class))).thenAnswer(invocation -> {
            RecordReminder reminder = invocation.getArgument(0);
            reminder.setId(9001L);
            return 1;
        });

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordReminderMapper).insert(org.mockito.ArgumentMatchers.argThat(reminder ->
                reminder.getRecordId().equals(404L)
                        && reminder.getUserId().equals(34L)
                        && "UNLOCK_REMINDER".equals(reminder.getTemplateType())
                        && reminder.getReminderStatus() == RecordReminderStatus.REQUESTED
                        && reminder.getLastError() == null));
        verify(wechatSubscribeMessageClient, never()).sendUnlockReminder(any(), any(), any());
    }

    @Test
    void shouldSendUnlockReminderWhenAuthorizationWasRecordedBeforeUnlock() {
        appWechatProperties.setUnlockReminderTemplateId("template-id");

        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(406L);
        expired.setUserId(36L);

        RecordReminder existing = new RecordReminder();
        existing.setId(9003L);
        existing.setRecordId(406L);
        existing.setUserId(36L);
        existing.setTemplateType("UNLOCK_REMINDER");
        existing.setReminderStatus(RecordReminderStatus.AUTHORIZED);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(406L), any(), any())).thenReturn(1);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(406L, "UNLOCK_REMINDER")).thenReturn(existing);
        when(userMapper.selectById(36L)).thenReturn(mockUser(36L, "openid-36"));

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordReminderMapper, never()).insert(any());
        verify(recordReminderMapper).updateStatusById(
                eq(9003L),
                eq(RecordReminderStatus.SEND_PENDING),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any());
        verify(wechatSubscribeMessageClient).sendUnlockReminder(eq("openid-36"), eq(406L), any());
        verify(recordReminderMapper).updateStatusById(
                eq(9003L),
                eq(RecordReminderStatus.SEND_SUCCESS),
                org.mockito.ArgumentMatchers.isNull(),
                any(),
                any());
    }

    @Test
    void shouldNotSendUnlockReminderWhenAuthorizationDenied() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(407L);
        expired.setUserId(37L);

        RecordReminder existing = new RecordReminder();
        existing.setId(9004L);
        existing.setRecordId(407L);
        existing.setUserId(37L);
        existing.setTemplateType("UNLOCK_REMINDER");
        existing.setReminderStatus(RecordReminderStatus.DENIED);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(407L), any(), any())).thenReturn(1);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(407L, "UNLOCK_REMINDER")).thenReturn(existing);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(userMapper, never()).selectById(any());
        verify(recordReminderMapper, never()).insert(any());
        verify(recordReminderMapper, never()).updateStatusById(any(), any(), any(), any(), any());
        verify(wechatSubscribeMessageClient, never()).sendUnlockReminder(any(), any(), any());
    }

    @Test
    void shouldNotSendUnlockReminderWhenAuthorizationOnlyRequested() {
        appWechatProperties.setUnlockReminderTemplateId("template-id");

        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(408L);
        expired.setUserId(38L);

        RecordReminder existing = new RecordReminder();
        existing.setId(9005L);
        existing.setRecordId(408L);
        existing.setUserId(38L);
        existing.setTemplateType("UNLOCK_REMINDER");
        existing.setReminderStatus(RecordReminderStatus.REQUESTED);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(408L), any(), any())).thenReturn(1);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(408L, "UNLOCK_REMINDER")).thenReturn(existing);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(userMapper, never()).selectById(any());
        verify(recordReminderMapper, never()).insert(any());
        verify(recordReminderMapper, never()).updateStatusById(any(), any(), any(), any(), any());
        verify(wechatSubscribeMessageClient, never()).sendUnlockReminder(any(), any(), any());
    }

    @Test
    void shouldRecordSendFailedWithoutBlockingUnlockWhenWechatSendFails() {
        appWechatProperties.setUnlockReminderTemplateId("template-id");

        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(405L);
        expired.setUserId(35L);
        RecordReminder existing = new RecordReminder();
        existing.setId(9002L);
        existing.setRecordId(405L);
        existing.setUserId(35L);
        existing.setTemplateType("UNLOCK_REMINDER");
        existing.setReminderStatus(RecordReminderStatus.AUTHORIZED);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired))
                .thenReturn(List.of());
        when(recordMapper.unlockSealedById(eq(405L), any(), any())).thenReturn(1);
        when(recordReminderMapper.selectByRecordIdAndTemplateType(405L, "UNLOCK_REMINDER")).thenReturn(existing);
        when(userMapper.selectById(35L)).thenReturn(mockUser(35L, "openid-35"));
        doThrow(new RuntimeException("wechat unavailable"))
                .when(wechatSubscribeMessageClient)
                .sendUnlockReminder(eq("openid-35"), eq(405L), any());

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordReminderMapper).updateStatusById(
                eq(9002L),
                eq(RecordReminderStatus.SEND_FAILED),
                eq("wechat unlock reminder send failed"),
                org.mockito.ArgumentMatchers.isNull(),
                any());
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

    private RecordAttachment attachment(Long id, RecordAttachmentType type) {
        RecordAttachment attachment = new RecordAttachment();
        attachment.setId(id);
        attachment.setRecordId(100L);
        attachment.setUserId(1L);
        attachment.setType(type);
        attachment.setStatus(RecordAttachmentStatus.AVAILABLE);
        attachment.setFileName(type == RecordAttachmentType.IMAGE ? "cover.jpg" : "voice.mp3");
        attachment.setMimeType(type == RecordAttachmentType.IMAGE ? "image/jpeg" : "audio/mpeg");
        attachment.setSizeBytes(123456L);
        attachment.setSortOrder(0);
        attachment.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 5, 0));
        return attachment;
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
