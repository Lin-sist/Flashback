package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.Reply;
import com.flashback.domain.ReplyType;
import com.flashback.dto.CreateReplyRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import com.flashback.service.ReplyService;
import com.flashback.vo.ReplyVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 回信模块核心业务实现。
 */
@Service
public class ReplyServiceImpl implements ReplyService {

    private static final ReplyType DEFAULT_REPLY_TYPE = ReplyType.SHORT_REPLY;

    private final RecordMapper recordMapper;
    private final ReplyMapper replyMapper;
    private final Clock clock;

    public ReplyServiceImpl(RecordMapper recordMapper, ReplyMapper replyMapper, Clock clock) {
        this.recordMapper = recordMapper;
        this.replyMapper = replyMapper;
        this.clock = clock;
    }

    @Override
    public ReplyVO create(Long userId, Long recordId, CreateReplyRequest request) {
        Record record = requireOwnedRecord(recordId, userId);
        ensureUnlocked(record);

        if (replyMapper.selectByRecordId(recordId) != null) {
            throw badRequest("回信已存在");
        }

        Reply reply = new Reply();
        reply.setRecordId(recordId);
        reply.setUserId(userId);
        reply.setContent(normalizeRequired(request.getContent(), "content不能为空"));
        reply.setReplyType(resolveReplyType(request.getReplyType()));
        reply.setCreatedAt(LocalDateTime.now(clock));

        try {
            replyMapper.insert(reply);
        } catch (DuplicateKeyException ex) {
            throw badRequest("回信已存在");
        }

        Reply created = replyMapper.selectByRecordId(recordId);
        return toReplyVO(created);
    }

    @Override
    public ReplyVO detail(Long userId, Long recordId) {
        requireOwnedRecord(recordId, userId);
        Reply reply = replyMapper.selectByRecordId(recordId);
        if (reply == null) {
            return null;
        }
        return toReplyVO(reply);
    }

    private Record requireOwnedRecord(Long recordId, Long userId) {
        Record record = recordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new NotFoundException("记录不存在");
        }
        return record;
    }

    private void ensureUnlocked(Record record) {
        if (record.getStatus() != RecordStatus.UNLOCKED) {
            throw badRequest("仅UNLOCKED状态允许回信");
        }
    }

    private ReplyType resolveReplyType(ReplyType replyType) {
        if (replyType == null) {
            return DEFAULT_REPLY_TYPE;
        }
        if (replyType != DEFAULT_REPLY_TYPE) {
            throw badRequest("replyType仅支持SHORT_REPLY");
        }
        return replyType;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw badRequest(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private ReplyVO toReplyVO(Reply reply) {
        ReplyVO vo = new ReplyVO();
        vo.setId(reply.getId());
        vo.setRecordId(reply.getRecordId());
        vo.setContent(reply.getContent());
        vo.setReplyType(reply.getReplyType());
        vo.setCreatedAt(reply.getCreatedAt());
        return vo;
    }
}
