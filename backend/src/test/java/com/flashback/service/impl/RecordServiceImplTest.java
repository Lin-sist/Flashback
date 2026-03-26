package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.UnlockNoticeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private RecordServiceImpl recordService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-26T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
        recordService = new RecordServiceImpl(recordMapper, unlockNoticeLogMapper, clock);
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
        when(recordMapper.countByUserAndCondition(1L, RecordStatus.DRAFT, RecordType.NODE_RECORD)).thenReturn(1L);
        when(recordMapper.selectPageByUserAndCondition(1L, RecordStatus.DRAFT, RecordType.NODE_RECORD, 0, 10))
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
    void shouldUnlockExpiredSealedRecordsAndWriteLog() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(201L);
        expired.setUserId(11L);
        expired.setUnlockAt(LocalDateTime.of(2026, 3, 26, 15, 0, 0));

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired), List.of());
        when(recordMapper.unlockSealedById(eq(201L), any(), any())).thenReturn(1);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(1);
        verify(recordMapper, times(1)).unlockSealedById(eq(201L), any(), any());
        verify(unlockNoticeLogMapper, times(1)).insert(any());
    }

    @Test
    void shouldNotUnlockWhenRecordNotExpired() {
        when(recordMapper.selectExpiredSealedRecords(any(), eq(100))).thenReturn(List.of());

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(0);
        verify(recordMapper, never()).unlockSealedById(any(), any(), any());
        verify(unlockNoticeLogMapper, never()).insert(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyUnlockedByAnotherRun() {
        Record expired = mockRecord(RecordStatus.SEALED);
        expired.setId(301L);
        expired.setUserId(21L);

        when(recordMapper.selectExpiredSealedRecords(any(), eq(100)))
                .thenReturn(List.of(expired), List.of());
        when(recordMapper.unlockSealedById(eq(301L), any(), any())).thenReturn(0);

        int unlockedCount = recordService.runUnlockJob();

        assertThat(unlockedCount).isEqualTo(0);
        verify(unlockNoticeLogMapper, never()).insert(any());
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
        record.setCreatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
        record.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
        return record;
    }

    private Record sealedRecord() {
        Record record = mockRecord(RecordStatus.SEALED);
        record.setSealedAt(LocalDateTime.of(2026, 3, 26, 16, 0, 0));
        return record;
    }
}
