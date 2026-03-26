package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.common.page.PageResult;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.RecordService;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录模块核心业务实现。
 */
@Service
public class RecordServiceImpl implements RecordService {

    private static final int PREVIEW_MAX_LENGTH = 60;

    private final RecordMapper recordMapper;
    private final Clock clock;

    public RecordServiceImpl(RecordMapper recordMapper, Clock clock) {
        this.recordMapper = recordMapper;
        this.clock = clock;
    }

    @Override
    public RecordDetailVO create(Long userId, CreateRecordRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);

        Record record = new Record();
        record.setUserId(userId);
        record.setTitle(normalizeOptional(request.getTitle()));
        record.setContent(normalizeRequired(request.getContent(), "content不能为空"));
        record.setRecordType(request.getRecordType());
        record.setCoreQuestion(normalizeOptional(request.getCoreQuestion()));
        record.setStatus(RecordStatus.DRAFT);
        record.setUnlockAt(request.getUnlockAt());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        recordMapper.insert(record);
        Record created = requireOwnedRecord(record.getId(), userId);
        return toDetailVO(created);
    }

    @Override
    public RecordDetailVO update(Long userId, Long id, UpdateRecordRequest request) {
        Record current = requireOwnedRecord(id, userId);
        ensureDraft(current, "仅DRAFT状态允许编辑");

        int affected = recordMapper.updateDraftByIdAndUserId(
                id,
                userId,
                normalizeOptional(request.getTitle()),
                normalizeRequired(request.getContent(), "content不能为空"),
                request.getRecordType(),
                normalizeOptional(request.getCoreQuestion()),
                request.getUnlockAt(),
                LocalDateTime.now(clock));
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    public void delete(Long userId, Long id) {
        Record current = requireOwnedRecord(id, userId);
        ensureDraft(current, "仅DRAFT状态允许删除");

        int affected = recordMapper.deleteDraftByIdAndUserId(id, userId);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }
    }

    @Override
    public RecordDetailVO seal(Long userId, Long id) {
        Record current = requireOwnedRecord(id, userId);
        ensureDraft(current, "仅DRAFT状态允许封存");

        if (normalizeOptional(current.getContent()) == null) {
            throw badRequest("封存前必须填写正文内容");
        }
        if (current.getUnlockAt() == null) {
            throw badRequest("封存前必须设置解锁时间");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!current.getUnlockAt().isAfter(now)) {
            throw badRequest("unlockAt必须晚于当前时间");
        }

        int affected = recordMapper.sealDraftByIdAndUserId(id, userId, now, now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    public PageResult<RecordListItemVO> pageMine(Long userId, RecordPageQuery query) {
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        long total = recordMapper.countByUserAndCondition(userId, query.getStatus(), query.getRecordType());
        List<Record> records = recordMapper.selectPageByUserAndCondition(
                userId,
                query.getStatus(),
                query.getRecordType(),
                offset,
                pageSize);

        List<RecordListItemVO> list = records.stream().map(this::toListItemVO).toList();
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public RecordDetailVO detail(Long userId, Long id) {
        return toDetailVO(requireOwnedRecord(id, userId));
    }

    private Record requireOwnedRecord(Long id, Long userId) {
        Record record = recordMapper.selectByIdAndUserId(id, userId);
        if (record == null) {
            throw new NotFoundException("记录不存在");
        }
        return record;
    }

    private void ensureDraft(Record record, String message) {
        if (record.getStatus() != RecordStatus.DRAFT) {
            throw badRequest(message);
        }
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
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

    private RecordListItemVO toListItemVO(Record record) {
        RecordListItemVO vo = new RecordListItemVO();
        vo.setId(record.getId());
        vo.setTitle(record.getTitle());
        vo.setContentPreview(toPreview(record.getContent()));
        vo.setRecordType(record.getRecordType());
        vo.setStatus(record.getStatus());
        vo.setUnlockAt(record.getUnlockAt());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }

    private String toPreview(String content) {
        String normalized = normalizeOptional(content);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    private RecordDetailVO toDetailVO(Record record) {
        RecordDetailVO vo = new RecordDetailVO();
        vo.setId(record.getId());
        vo.setTitle(record.getTitle());
        vo.setContent(record.getContent());
        vo.setRecordType(record.getRecordType());
        vo.setCoreQuestion(record.getCoreQuestion());
        vo.setStatus(record.getStatus());
        vo.setUnlockAt(record.getUnlockAt());
        vo.setSealedAt(record.getSealedAt());
        vo.setUnlockedAt(record.getUnlockedAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }
}
