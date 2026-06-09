package com.flashback.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.common.page.PageResult;
import com.flashback.config.AppWechatProperties;
import com.flashback.domain.LifeNodeType;
import com.flashback.domain.Record;
import com.flashback.domain.RecordReminder;
import com.flashback.domain.RecordReminderStatus;
import com.flashback.domain.RecordTagName;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.Tag;
import com.flashback.domain.UnlockNoticeLog;
import com.flashback.domain.User;
import com.flashback.mapper.RecordReminderMapper;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateLaterReflectionRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.UnlockNoticeLogMapper;
import com.flashback.mapper.UserMapper;
import com.flashback.service.RecordService;
import com.flashback.wechat.WechatSubscribeMessageClient;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.RecordTagVO;
import com.flashback.vo.TimelineGroupVO;
import com.flashback.vo.TimelineItemVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 记录模块核心业务实现。
 */
@Service
public class RecordServiceImpl implements RecordService {

    private static final int PREVIEW_MAX_LENGTH = 60;
    private static final int UNLOCK_BATCH_SIZE = 100;
    private static final String NOTICE_TYPE_SYSTEM_UNLOCK = "SYSTEM_UNLOCK";
    private static final String NOTICE_STATUS_SUCCESS = "SUCCESS";
    private static final String TEMPLATE_TYPE_UNLOCK_REMINDER = "UNLOCK_REMINDER";
    private static final String OPENID_NOT_BOUND_MESSAGE = "openid not bound";
    private static final String TEMPLATE_NOT_CONFIGURED_MESSAGE = "wechat unlock reminder template not configured";
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RecordMapper recordMapper;
    private final TagMapper tagMapper;
    private final RecordTagMapper recordTagMapper;
    private final ReplyMapper replyMapper;
    private final UnlockNoticeLogMapper unlockNoticeLogMapper;
    private final RecordReminderMapper recordReminderMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final AppWechatProperties appWechatProperties;
    private final WechatSubscribeMessageClient wechatSubscribeMessageClient;
    private final Clock clock;

    public RecordServiceImpl(
            RecordMapper recordMapper,
            TagMapper tagMapper,
            RecordTagMapper recordTagMapper,
            ReplyMapper replyMapper,
            UnlockNoticeLogMapper unlockNoticeLogMapper,
            RecordReminderMapper recordReminderMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            AppWechatProperties appWechatProperties,
            WechatSubscribeMessageClient wechatSubscribeMessageClient,
            Clock clock) {
        this.recordMapper = recordMapper;
        this.tagMapper = tagMapper;
        this.recordTagMapper = recordTagMapper;
        this.replyMapper = replyMapper;
        this.unlockNoticeLogMapper = unlockNoticeLogMapper;
        this.recordReminderMapper = recordReminderMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.appWechatProperties = appWechatProperties;
        this.wechatSubscribeMessageClient = wechatSubscribeMessageClient;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RecordDetailVO create(Long userId, CreateRecordRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIdsExist(tagIds);

        Record record = new Record();
        record.setUserId(userId);
        record.setTitle(normalizeOptional(request.getTitle()));
        record.setContent(normalizeRequired(request.getContent(), "content不能为空"));
        record.setRecordType(request.getRecordType());
        record.setCoreQuestion(normalizeOptional(request.getCoreQuestion()));
        record.setAiSummary(normalizeOptional(request.getAiSummary()));
        record.setAiPromptResult(serializeAiPromptResults(request.getAiPromptResults()));
        record.setBeliefThen(normalizeOptional(request.getBeliefThen()));
        record.setLifeNodeType(request.getLifeNodeType());
        record.setLifeNodeCustomLabel(validateLifeNodeCustomLabel(
                request.getLifeNodeType(),
                request.getLifeNodeCustomLabel()));
        record.setStatus(RecordStatus.DRAFT);
        record.setUnlockAt(request.getUnlockAt());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        recordMapper.insert(record);
        rebindRecordTags(record.getId(), tagIds);
        Record created = requireOwnedRecord(record.getId(), userId);
        return toDetailVO(created);
    }

    @Override
    @Transactional
    public RecordDetailVO update(Long userId, Long id, UpdateRecordRequest request) {
        Record current = requireOwnedRecord(id, userId);
        ensureDraft(current, "仅DRAFT状态允许编辑");
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIdsExist(tagIds);

        int affected = recordMapper.updateDraftByIdAndUserId(
                id,
                userId,
                normalizeOptional(request.getTitle()),
                normalizeRequired(request.getContent(), "content不能为空"),
                request.getRecordType(),
                normalizeOptional(request.getCoreQuestion()),
                normalizeOptional(request.getAiSummary()),
                serializeAiPromptResults(request.getAiPromptResults()),
                normalizeOptional(request.getBeliefThen()),
                request.getLifeNodeType(),
                validateLifeNodeCustomLabel(request.getLifeNodeType(), request.getLifeNodeCustomLabel()),
                request.getUnlockAt(),
                LocalDateTime.now(clock));
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        rebindRecordTags(id, tagIds);

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Record current = requireOwnedRecord(id, userId);
        ensureDraft(current, "仅DRAFT状态允许删除");

        int affected = recordMapper.deleteDraftByIdAndUserId(id, userId);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }
        recordTagMapper.deleteByRecordId(id);
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
    @Transactional
    public RecordDetailVO updateLaterReflection(Long userId, Long id, UpdateLaterReflectionRequest request) {
        Record current = requireOwnedRecord(id, userId);
        if (current.getStatus() != RecordStatus.UNLOCKED) {
            throw badRequest("仅UNLOCKED状态允许填写后来其实");
        }
        if (laterReflectionSubmitCount(current) >= 2) {
            throw badRequest("后来其实提交次数已用完");
        }

        int affected = recordMapper.updateLaterReflectionByIdAndUserId(
                id,
                userId,
                normalizeRequired(request.getRealityLater(), "realityLater不能为空"),
                LocalDateTime.now(clock));
        if (affected == 0) {
            throw badRequest("后来其实提交次数已用完");
        }

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    public PageResult<RecordListItemVO> pageMine(Long userId, RecordPageQuery query) {
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        String keyword = normalizeOptional(query.getKeyword());

        long total = recordMapper.countByUserAndCondition(
                userId,
                query.getStatus(),
                query.getRecordType(),
                query.getTagId(),
                keyword);
        List<Record> records = recordMapper.selectPageByUserAndCondition(
                userId,
                query.getStatus(),
                query.getRecordType(),
                query.getTagId(),
                keyword,
                offset,
                pageSize);

        Map<Long, List<String>> tagNamesByRecordId = loadTagNamesByRecordIds(records);
        List<RecordListItemVO> list = records.stream()
                .map(record -> toListItemVO(record, tagNamesByRecordId.getOrDefault(record.getId(), List.of())))
                .toList();
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public PageResult<RecordListItemVO> pageMyUnlocked(Long userId, RecordPageQuery query) {
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        long total = recordMapper.countUnlockedByUser(userId);
        List<Record> records = recordMapper.selectUnlockedPageByUser(userId, offset, pageSize);
        Map<Long, List<String>> tagNamesByRecordId = loadTagNamesByRecordIds(records);
        List<RecordListItemVO> list = records.stream()
                .map(record -> toListItemVO(record, tagNamesByRecordId.getOrDefault(record.getId(), List.of())))
                .toList();
        return PageResult.of(list, total, pageNum, pageSize);
    }

    @Override
    public List<TimelineGroupVO> timeline(Long userId, RecordTimelineQuery query) {
        List<Record> records = recordMapper.selectTimelineByUserAndCondition(userId, query.getTagId(), query.getYear());
        Map<Long, List<String>> tagNamesByRecordId = loadTagNamesByRecordIds(records);

        Map<String, List<TimelineItemVO>> grouped = new LinkedHashMap<>();
        for (Record record : records) {
            String yearMonth = record.getCreatedAt().format(YEAR_MONTH_FORMATTER);
            TimelineItemVO item = toTimelineItemVO(record, tagNamesByRecordId.getOrDefault(record.getId(), List.of()));
            grouped.computeIfAbsent(yearMonth, key -> new ArrayList<>()).add(item);
        }

        List<TimelineGroupVO> timeline = new ArrayList<>();
        for (Map.Entry<String, List<TimelineItemVO>> entry : grouped.entrySet()) {
            TimelineGroupVO group = new TimelineGroupVO();
            group.setYearMonth(entry.getKey());
            group.setItems(entry.getValue());
            timeline.add(group);
        }
        return timeline;
    }

    @Override
    @Transactional
    public int runUnlockJob() {
        int unlockedCount = 0;
        LocalDateTime now = LocalDateTime.now(clock);

        while (true) {
            List<Record> expiredSealedRecords = recordMapper.selectExpiredSealedRecords(now, UNLOCK_BATCH_SIZE);
            if (expiredSealedRecords.isEmpty()) {
                break;
            }

            for (Record record : expiredSealedRecords) {
                int affected = recordMapper.unlockSealedById(record.getId(), now, now);
                if (affected == 1) {
                    insertUnlockNoticeLog(record.getId(), record.getUserId(), now);
                    createUnlockReminderIfAbsent(record.getId(), record.getUserId(), now);
                    unlockedCount++;
                }
            }

            if (expiredSealedRecords.size() < UNLOCK_BATCH_SIZE) {
                break;
            }
        }

        return unlockedCount;
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

    private int laterReflectionSubmitCount(Record record) {
        return record.getRealityLaterSubmitCount() == null ? 0 : record.getRealityLaterSubmitCount();
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private String validateLifeNodeCustomLabel(LifeNodeType lifeNodeType, String customLabel) {
        String normalizedLabel = normalizeOptional(customLabel);
        if (normalizedLabel != null && lifeNodeType != LifeNodeType.OTHER) {
            throw badRequest("lifeNodeCustomLabel仅在lifeNodeType为OTHER时允许填写");
        }
        return normalizedLabel;
    }

    private void insertUnlockNoticeLog(Long recordId, Long userId, LocalDateTime createdAt) {
        UnlockNoticeLog unlockNoticeLog = new UnlockNoticeLog();
        unlockNoticeLog.setRecordId(recordId);
        unlockNoticeLog.setUserId(userId);
        unlockNoticeLog.setNoticeType(NOTICE_TYPE_SYSTEM_UNLOCK);
        unlockNoticeLog.setNoticeStatus(NOTICE_STATUS_SUCCESS);
        unlockNoticeLog.setCreatedAt(createdAt);
        unlockNoticeLogMapper.insert(unlockNoticeLog);
    }

    private void createUnlockReminderIfAbsent(Long recordId, Long userId, LocalDateTime now) {
        try {
            RecordReminder existing = recordReminderMapper.selectByRecordIdAndTemplateType(
                    recordId,
                    TEMPLATE_TYPE_UNLOCK_REMINDER);
            if (existing != null) {
                return;
            }

            User user = userMapper.selectById(userId);
            String openid = user == null ? null : normalizeOptional(user.getOpenid());

            RecordReminder reminder = new RecordReminder();
            reminder.setRecordId(recordId);
            reminder.setUserId(userId);
            reminder.setTemplateType(TEMPLATE_TYPE_UNLOCK_REMINDER);
            reminder.setCreatedAt(now);
            reminder.setUpdatedAt(now);
            if (openid == null) {
                reminder.setReminderStatus(RecordReminderStatus.SKIPPED_NO_OPENID);
                reminder.setLastError(OPENID_NOT_BOUND_MESSAGE);
            } else if (!appWechatProperties.hasUnlockReminderTemplate()) {
                reminder.setReminderStatus(RecordReminderStatus.NOT_CONFIGURED);
                reminder.setLastError(TEMPLATE_NOT_CONFIGURED_MESSAGE);
            } else {
                reminder.setReminderStatus(RecordReminderStatus.SEND_PENDING);
            }

            recordReminderMapper.insert(reminder);
            if (reminder.getReminderStatus() == RecordReminderStatus.SEND_PENDING) {
                sendUnlockReminder(reminder, openid, now);
            }
        } catch (Exception ex) {
            // Reminder persistence is best-effort in M2 and must never block unlock processing.
        }
    }

    private void sendUnlockReminder(RecordReminder reminder, String openid, LocalDateTime now) {
        try {
            wechatSubscribeMessageClient.sendUnlockReminder(openid, reminder.getRecordId(), now);
            recordReminderMapper.updateStatusById(
                    reminder.getId(),
                    RecordReminderStatus.SEND_SUCCESS,
                    null,
                    now,
                    now);
        } catch (Exception ex) {
            recordReminderMapper.updateStatusById(
                    reminder.getId(),
                    RecordReminderStatus.SEND_FAILED,
                    "wechat unlock reminder send failed",
                    null,
                    now);
        }
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

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId == null) {
                throw badRequest("tagIds存在空值");
            }
            unique.add(tagId);
        }
        return List.copyOf(unique);
    }

    private void validateTagIdsExist(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        long count = tagMapper.countEnabledByIds(tagIds);
        if (count != tagIds.size()) {
            throw badRequest("标签不存在");
        }
    }

    private void rebindRecordTags(Long recordId, List<Long> tagIds) {
        recordTagMapper.deleteByRecordId(recordId);
        if (!tagIds.isEmpty()) {
            recordTagMapper.batchInsert(recordId, tagIds);
        }
    }

    private Map<Long, List<String>> loadTagNamesByRecordIds(List<Record> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }

        List<Long> recordIds = records.stream().map(Record::getId).toList();
        List<RecordTagName> rows = tagMapper.selectTagNamesByRecordIds(recordIds);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (RecordTagName row : rows) {
            result.computeIfAbsent(row.getRecordId(), key -> new ArrayList<>()).add(row.getTagName());
        }
        return result;
    }

    private List<RecordTagVO> loadRecordTags(Long recordId) {
        List<Tag> tags = tagMapper.selectTagsByRecordId(recordId);
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream().map(this::toRecordTagVO).toList();
    }

    private RecordTagVO toRecordTagVO(Tag tag) {
        RecordTagVO vo = new RecordTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setType(tag.getType());
        return vo;
    }

    private RecordListItemVO toListItemVO(Record record, List<String> tagNames) {
        RecordListItemVO vo = new RecordListItemVO();
        vo.setId(record.getId());
        vo.setTitle(record.getTitle());
        vo.setContentPreview(toPreview(record.getContent()));
        vo.setRecordType(record.getRecordType());
        vo.setStatus(record.getStatus());
        vo.setLifeNodeLabel(resolveLifeNodeLabel(record));
        vo.setUnlockAt(record.getUnlockAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setTagNames(tagNames);
        return vo;
    }

    private TimelineItemVO toTimelineItemVO(Record record, List<String> tagNames) {
        TimelineItemVO item = new TimelineItemVO();
        item.setId(record.getId());
        item.setTitle(record.getTitle());
        item.setStatus(record.getStatus());
        item.setRecordType(record.getRecordType());
        item.setLifeNodeLabel(resolveLifeNodeLabel(record));
        item.setCreatedAt(record.getCreatedAt());
        item.setTagNames(tagNames);
        return item;
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
        vo.setAiSummary(record.getAiSummary());
        vo.setAiPromptResults(deserializeAiPromptResults(record.getAiPromptResult()));
        vo.setBeliefThen(record.getBeliefThen());
        vo.setRealityLater(record.getRealityLater());
        vo.setLifeNodeType(record.getLifeNodeType());
        vo.setLifeNodeCustomLabel(record.getLifeNodeCustomLabel());
        vo.setLifeNodeLabel(resolveLifeNodeLabel(record));
        vo.setTags(loadRecordTags(record.getId()));
        boolean hasReply = record.getStatus() == RecordStatus.UNLOCKED
                && replyMapper.selectByRecordId(record.getId()) != null;
        vo.setHasReply(hasReply);
        vo.setCanReply(record.getStatus() == RecordStatus.UNLOCKED && !hasReply);
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private String resolveLifeNodeLabel(Record record) {
        LifeNodeType lifeNodeType = record.getLifeNodeType();
        if (lifeNodeType == null) {
            return null;
        }
        if (lifeNodeType == LifeNodeType.OTHER) {
            String customLabel = normalizeOptional(record.getLifeNodeCustomLabel());
            return customLabel == null ? LifeNodeType.OTHER.getLabel() : customLabel;
        }
        return lifeNodeType.getLabel();
    }

    private String serializeAiPromptResults(List<String> aiPromptResults) {
        List<String> normalized = normalizeAiPromptResults(aiPromptResults);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException ex) {
            throw badRequest("aiPromptResults格式错误");
        }
    }

    private List<String> deserializeAiPromptResults(String rawValue) {
        String normalized = normalizeOptional(rawValue);
        if (normalized == null) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(normalized, STRING_LIST_TYPE);
            return normalizeAiPromptResults(values);
        } catch (Exception ex) {
            return List.of(normalized);
        }
    }

    private List<String> normalizeAiPromptResults(List<String> aiPromptResults) {
        if (aiPromptResults == null || aiPromptResults.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : aiPromptResults) {
            String value = normalizeOptional(item);
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }
}
