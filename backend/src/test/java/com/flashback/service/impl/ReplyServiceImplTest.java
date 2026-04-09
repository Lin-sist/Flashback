package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.Reply;
import com.flashback.domain.ReplyType;
import com.flashback.dto.CreateReplyRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyServiceImplTest {

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private ReplyMapper replyMapper;

    private ReplyServiceImpl replyService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-09T12:00:00Z"), ZoneId.of("Asia/Shanghai"));
        replyService = new ReplyServiceImpl(recordMapper, replyMapper, clock);
    }

    @Test
    void shouldRejectCreateWhenRecordNotOwned() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(null);

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("test");

        assertThatThrownBy(() -> replyService.create(1L, 100L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
    }

    @Test
    void shouldRejectCreateWhenRecordNotUnlocked() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.SEALED));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("test");

        assertThatThrownBy(() -> replyService.create(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("仅UNLOCKED状态允许回信");
    }

    @Test
    void shouldRejectCreateWhenReplyAlreadyExists() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L)).thenReturn(mockReply(100L, 1L, "exists"));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("new");

        assertThatThrownBy(() -> replyService.create(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("回信已存在");
        verify(replyMapper, never()).insert(any());
    }

    @Test
    void shouldCreateReplySuccessfully() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L))
                .thenReturn(null, mockReply(100L, 1L, "你做到了"));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("  你做到了  ");

        var result = replyService.create(1L, 100L, request);

        assertThat(result).isNotNull();
        assertThat(result.getRecordId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("你做到了");
        assertThat(result.getReplyType()).isEqualTo(ReplyType.SHORT_REPLY);
        verify(replyMapper).insert(any(Reply.class));
    }

    @Test
    void shouldDefaultReplyTypeWhenReplyTypeIsNull() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L))
                .thenReturn(null, mockReply(100L, 1L, "ok"));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("ok");
        request.setReplyType(null);

        var result = replyService.create(1L, 100L, request);
        assertThat(result.getReplyType()).isEqualTo(ReplyType.SHORT_REPLY);
    }

    @Test
    void shouldMapDuplicateKeyToBizException() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);
        when(replyMapper.insert(any(Reply.class))).thenThrow(new DuplicateKeyException("duplicate"));

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("test");

        assertThatThrownBy(() -> replyService.create(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("回信已存在");
    }

    @Test
    void shouldRejectCreateWhenContentBlankAfterTrim() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        CreateReplyRequest request = new CreateReplyRequest();
        request.setContent("   ");

        assertThatThrownBy(() -> replyService.create(1L, 100L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("content不能为空");
        verify(replyMapper, never()).insert(any(Reply.class));
    }

    @Test
    void shouldReturnNullWhenReplyNotFound() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L)).thenReturn(null);

        var result = replyService.detail(1L, 100L);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnReplyWhenFound() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(mockRecord(RecordStatus.UNLOCKED));
        when(replyMapper.selectByRecordId(100L)).thenReturn(mockReply(100L, 1L, "hello"));

        var result = replyService.detail(1L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("hello");
    }

    @Test
    void shouldRejectDetailWhenRecordNotOwned() {
        when(recordMapper.selectByIdAndUserId(100L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> replyService.detail(1L, 100L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
    }

    private Record mockRecord(RecordStatus status) {
        Record record = new Record();
        record.setId(100L);
        record.setUserId(1L);
        record.setStatus(status);
        return record;
    }

    private Reply mockReply(Long recordId, Long userId, String content) {
        Reply reply = new Reply();
        reply.setId(200L);
        reply.setRecordId(recordId);
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setReplyType(ReplyType.SHORT_REPLY);
        reply.setCreatedAt(LocalDateTime.of(2026, 4, 9, 20, 0, 0));
        return reply;
    }
}
