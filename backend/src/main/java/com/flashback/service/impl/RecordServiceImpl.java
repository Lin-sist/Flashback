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
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordLocation;
import com.flashback.domain.RecordLocationSource;
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
import com.flashback.dto.UpdateRecordCoverRequest;
import com.flashback.dto.UpdateRecordLocationRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.dto.UpdateUnlockReminderAuthorizationRequest;
import com.flashback.mapper.RecordLocationMapper;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.ReplyMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.UnlockNoticeLogMapper;
import com.flashback.mapper.UserMapper;
import com.flashback.service.RecordService;
import com.flashback.service.RecordSaveEligibility;
import com.flashback.service.data.DataOwnershipMutationGuard;
import com.flashback.wechat.WechatSubscribeMessageClient;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordAttachmentVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.RecordLocationVO;
import com.flashback.vo.RecordTagVO;
import com.flashback.vo.TimelineGroupVO;
import com.flashback.vo.TimelineItemVO;
import com.flashback.vo.TimelinePageVO;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
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
    private static final long DRAFT_RECOVERY_DAYS = 7L;
    /**
     * 与 UpdateRecordRequest / CreateRecordRequest 的 tagIds @Size(max = 20) 保持一致。
     */
    private static final int MAX_TAG_IDS_PER_RECORD = 20;
    private static final String NOTICE_TYPE_SYSTEM_UNLOCK = "SYSTEM_UNLOCK";
    private static final String NOTICE_STATUS_SUCCESS = "SUCCESS";
    private static final String TEMPLATE_TYPE_UNLOCK_REMINDER = "UNLOCK_REMINDER";
    private static final String OPENID_NOT_BOUND_MESSAGE = "openid not bound";
    private static final String TEMPLATE_NOT_CONFIGURED_MESSAGE = "wechat unlock reminder template not configured";
    private static final String AUTHORIZATION_UNAVAILABLE_MESSAGE = "wechat subscription authorization unavailable";
    private static final String AUTHORIZATION_DENIED_MESSAGE = "wechat subscription authorization denied";
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RecordMapper recordMapper;
    private final TagMapper tagMapper;
    private final RecordTagMapper recordTagMapper;
    private final RecordLocationMapper recordLocationMapper;
    private final RecordAttachmentMapper recordAttachmentMapper;
    private final ReplyMapper replyMapper;
    private final UnlockNoticeLogMapper unlockNoticeLogMapper;
    private final RecordReminderMapper recordReminderMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final AppWechatProperties appWechatProperties;
    private final WechatSubscribeMessageClient wechatSubscribeMessageClient;
    private final Clock clock;
    private final RecordSaveEligibility recordSaveEligibility;
    private DataOwnershipMutationGuard dataOwnershipMutationGuard;

    @Autowired
    void setDataOwnershipMutationGuard(DataOwnershipMutationGuard guard) {
        this.dataOwnershipMutationGuard = guard;
    }

    public RecordServiceImpl(
            RecordMapper recordMapper,
            TagMapper tagMapper,
            RecordTagMapper recordTagMapper,
            RecordLocationMapper recordLocationMapper,
            RecordAttachmentMapper recordAttachmentMapper,
            ReplyMapper replyMapper,
            UnlockNoticeLogMapper unlockNoticeLogMapper,
            RecordReminderMapper recordReminderMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            AppWechatProperties appWechatProperties,
            WechatSubscribeMessageClient wechatSubscribeMessageClient,
            Clock clock,
            RecordSaveEligibility recordSaveEligibility) {
        this.recordMapper = recordMapper;
        this.tagMapper = tagMapper;
        this.recordTagMapper = recordTagMapper;
        this.recordLocationMapper = recordLocationMapper;
        this.recordAttachmentMapper = recordAttachmentMapper;
        this.replyMapper = replyMapper;
        this.unlockNoticeLogMapper = unlockNoticeLogMapper;
        this.recordReminderMapper = recordReminderMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.appWechatProperties = appWechatProperties;
        this.wechatSubscribeMessageClient = wechatSubscribeMessageClient;
        this.clock = clock;
        this.recordSaveEligibility = recordSaveEligibility;
    }

    @Override
    @Transactional
    public RecordDetailVO create(Long userId, CreateRecordRequest request) {
        assertOwnershipWritable(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIdsExist(tagIds);

        Record record = new Record();
        record.setUserId(userId);
        record.setTitle(normalizeOptional(request.getTitle()));
        record.setContent(normalizeContent(request.getContent()));
        record.setRecordType(request.getRecordType() == null ? com.flashback.domain.RecordType.MOMENT : request.getRecordType());
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
        record.setDraftExpiresAt(now.plusDays(DRAFT_RECOVERY_DAYS));
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
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "仅DRAFT或SAVED状态允许编辑");
        List<Long> tagIds = normalizeTagIds(request.getTagIds());
        validateTagIdsExist(tagIds);
        String normalizedContent = normalizeContent(request.getContent());
        if (current.getStatus() == RecordStatus.SAVED
                && !recordSaveEligibility.isEligible(current, normalizedContent)) {
            throw badRequest("至少留下一句话、一张图片或一段声音");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        int affected = recordMapper.updateEditableByIdAndUserId(
                id,
                userId,
                normalizeOptional(request.getTitle()),
                normalizedContent,
                request.getRecordType(),
                normalizeOptional(request.getCoreQuestion()),
                normalizeOptional(request.getAiSummary()),
                serializeAiPromptResults(request.getAiPromptResults()),
                normalizeOptional(request.getBeliefThen()),
                request.getLifeNodeType(),
                validateLifeNodeCustomLabel(request.getLifeNodeType(), request.getLifeNodeCustomLabel()),
                request.getUnlockAt(),
                now,
                current.getStatus() == RecordStatus.DRAFT ? now.plusDays(DRAFT_RECOVERY_DAYS) : null);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        rebindRecordTags(id, tagIds);

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, "请通过数据与所有权的二次确认流程删除记录");
    }

    @Override
    @Transactional
    public RecordDetailVO updateLocation(Long userId, Long id, UpdateRecordLocationRequest request) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能编辑位置");
        validateLocation(request);

        LocalDateTime now = LocalDateTime.now(clock);
        RecordLocation location = new RecordLocation();
        location.setRecordId(id);
        location.setUserId(userId);
        location.setSource(request.getSource());
        location.setName(normalizeOptional(request.getName()));
        location.setAddress(normalizeOptional(request.getAddress()));
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setCreatedAt(now);
        location.setUpdatedAt(now);
        recordLocationMapper.upsert(location);
        touchDraft(current, now);

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO deleteLocation(Long userId, Long id) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能删除位置");

        recordLocationMapper.deleteByRecordIdAndUserId(id, userId);
        touchDraft(current, LocalDateTime.now(clock));
        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO updateCover(Long userId, Long id, UpdateRecordCoverRequest request) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能设置封面");

        Long attachmentId = request == null ? null : request.getAttachmentId();
        if (attachmentId != null) {
            RecordAttachment attachment = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(
                    attachmentId,
                    id,
                    userId);
            if (attachment == null || attachment.getStatus() != RecordAttachmentStatus.AVAILABLE) {
                throw new NotFoundException("封面附件不存在");
            }
            if (attachment.getType() != RecordAttachmentType.IMAGE) {
                throw badRequest("封面必须选择图片附件");
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int affected = recordMapper.updateCoverAttachmentByIdAndUserId(
                id,
                userId,
                attachmentId,
                now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }
        touchDraft(current, now);
        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    public RecordDetailVO seal(Long userId, Long id) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        if (current.getStatus() != RecordStatus.SAVED) {
            throw badRequest("仅SAVED状态允许封存");
        }
        if (current.getUnlockAt() == null) {
            throw badRequest("封存前必须设置解锁时间");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!current.getUnlockAt().isAfter(now)) {
            throw badRequest("unlockAt必须晚于当前时间");
        }

        int affected = recordMapper.sealSavedByIdAndUserId(id, userId, now, now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO appendContent(Long userId, Long id, String text) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能追加正文");

        String addition = normalizeRequired(text, "追加内容不能为空");
        String existing = normalizeOptional(current.getContent());
        // 只追加：既有正文原样保留在前，不做修剪、润色或替换。
        String merged = existing == null ? addition : existing + "\n\n" + addition;

        LocalDateTime now = LocalDateTime.now(clock);
        int affected = recordMapper.updateDraftContentByIdAndUserId(
                id,
                userId,
                merged,
                now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        touchDraft(current, now);

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO appendTags(Long userId, Long id, List<Long> tagIds) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能修改标签");

        List<Long> incoming = normalizeTagIds(tagIds);
        if (incoming.isEmpty()) {
            throw badRequest("tagIds不能为空");
        }
        validateTagIdsExist(incoming);

        List<Long> existing = recordTagMapper.selectTagIdsByRecordId(id);
        LinkedHashSet<Long> merged = new LinkedHashSet<>(existing == null ? List.of() : existing);
        // 只追加：既有标签先入集合，重复项被集合天然去重，不产生重复绑定。
        merged.addAll(incoming);

        if (merged.size() > MAX_TAG_IDS_PER_RECORD) {
            throw badRequest("标签数量超出上限");
        }

        if (merged.size() != (existing == null ? 0 : existing.size())) {
            rebindRecordTags(id, List.copyOf(merged));
        }

        touchDraft(current, LocalDateTime.now(clock));

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO updateUnlockAt(Long userId, Long id, LocalDateTime unlockAt) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        ensureEditable(current, "记录已封存，不能修改解锁时间");

        if (unlockAt == null) {
            throw badRequest("unlockAt不能为空");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!unlockAt.isAfter(now)) {
            throw badRequest("unlockAt必须晚于当前时间");
        }

        // 只写解锁时间，不改 status：封存仍须用户自行确认。
        int affected = recordMapper.updateDraftUnlockAtByIdAndUserId(id, userId, unlockAt, now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }

        touchDraft(current, now);

        return toDetailVO(requireOwnedRecord(id, userId));
    }

    @Override
    @Transactional
    public RecordDetailVO updateLaterReflection(Long userId, Long id, UpdateLaterReflectionRequest request) {
        assertOwnershipWritable(userId);
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
    @Transactional
    public RecordDetailVO updateUnlockReminderAuthorization(
            Long userId,
            Long id,
            UpdateUnlockReminderAuthorizationRequest request) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        if (current.getStatus() == RecordStatus.DRAFT || current.getStatus() == RecordStatus.SAVED) {
            throw badRequest("仅封存后的记录允许更新提醒授权状态");
        }

        RecordReminderStatus status = request.getStatus();
        if (!isAuthorizationStatus(status)) {
            throw badRequest("提醒授权状态仅支持REQUESTED、AUTHORIZED或DENIED");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        RecordReminder existing = recordReminderMapper.selectByRecordIdAndTemplateType(
                id,
                TEMPLATE_TYPE_UNLOCK_REMINDER);
        String lastError = authorizationLastError(status);
        if (existing == null) {
            RecordReminder reminder = new RecordReminder();
            reminder.setRecordId(id);
            reminder.setUserId(userId);
            reminder.setTemplateType(TEMPLATE_TYPE_UNLOCK_REMINDER);
            reminder.setReminderStatus(status);
            reminder.setLastError(lastError);
            reminder.setCreatedAt(now);
            reminder.setUpdatedAt(now);
            recordReminderMapper.insert(reminder);
        } else if (existing.getReminderStatus() != RecordReminderStatus.SEND_SUCCESS) {
            recordReminderMapper.updateStatusById(
                    existing.getId(),
                    status,
                    lastError,
                    null,
                    now);
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
    public TimelinePageVO timeline(Long userId, RecordTimelineQuery query) {
        TimelineDateRange dateRange = resolveTimelineDateRange(query);
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        long total = recordMapper.countTimelineByUserAndCondition(
                userId,
                query.getTagId(),
                dateRange.createdFrom(),
                dateRange.createdBefore());
        List<Record> records = total == 0
                ? List.of()
                : recordMapper.selectTimelinePageByUserAndCondition(
                        userId,
                        query.getTagId(),
                        dateRange.createdFrom(),
                        dateRange.createdBefore(),
                        offset,
                        pageSize);
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
        return TimelinePageVO.of(timeline, total, pageNum, pageSize);
    }

    private TimelineDateRange resolveTimelineDateRange(RecordTimelineQuery query) {
        Integer year = query.getYear();
        Integer month = query.getMonth();
        Integer day = query.getDay();
        if (year == null) {
            if (month != null || day != null) {
                throw badRequest("年月日筛选条件无效");
            }
            return new TimelineDateRange(null, null);
        }
        if (day != null && month == null) {
            throw badRequest("年月日筛选条件无效");
        }

        try {
            LocalDate startDate;
            LocalDate endDate;
            if (month == null) {
                startDate = LocalDate.of(year, 1, 1);
                endDate = startDate.plusYears(1);
            } else if (day == null) {
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.plusMonths(1);
            } else {
                startDate = LocalDate.of(year, month, day);
                endDate = startDate.plusDays(1);
            }
            return new TimelineDateRange(
                    startDate.atStartOfDay(clock.getZone()).toLocalDateTime(),
                    endDate.atStartOfDay(clock.getZone()).toLocalDateTime());
        } catch (DateTimeException ex) {
            throw badRequest("年月日筛选条件无效");
        }
    }

    private record TimelineDateRange(LocalDateTime createdFrom, LocalDateTime createdBefore) {
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
        if (record.getStatus() == RecordStatus.DRAFT
                && record.getDraftExpiresAt() != null
                && !record.getDraftExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new NotFoundException("记录不存在");
        }
        return record;
    }

    private void ensureActiveDraft(Record record, String message) {
        if (record.getStatus() != RecordStatus.DRAFT
                || (record.getDraftExpiresAt() != null
                && !record.getDraftExpiresAt().isAfter(LocalDateTime.now(clock)))) {
            throw badRequest(message);
        }
    }

    private void touchDraft(Record record, LocalDateTime now) {
        if (record.getStatus() != RecordStatus.DRAFT && record.getStatus() != RecordStatus.SAVED) {
            return;
        }
        int affected = recordMapper.touchDraftByIdAndUserId(
                record.getId(),
                record.getUserId(),
                now,
                now.plusDays(DRAFT_RECOVERY_DAYS));
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }
    }

    private int laterReflectionSubmitCount(Record record) {
        return record.getRealityLaterSubmitCount() == null ? 0 : record.getRealityLaterSubmitCount();
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private void assertOwnershipWritable(Long userId) {
        if (dataOwnershipMutationGuard != null) dataOwnershipMutationGuard.assertWritable(userId);
    }

    private void validateLocation(UpdateRecordLocationRequest request) {
        if (request.getSource() == RecordLocationSource.CURRENT_LOCATION
                || request.getSource() == RecordLocationSource.MAP_PICKER) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw badRequest("CURRENT_LOCATION和MAP_PICKER位置必须包含latitude和longitude");
            }
            validateCoordinate(request.getLatitude(), BigDecimal.valueOf(-90), BigDecimal.valueOf(90), "latitude");
            validateCoordinate(request.getLongitude(), BigDecimal.valueOf(-180), BigDecimal.valueOf(180), "longitude");
            return;
        }

        String name = normalizeOptional(request.getName());
        String address = normalizeOptional(request.getAddress());
        if (name == null && address == null) {
            throw badRequest("MANUAL位置必须填写name或address");
        }
        if (request.getLatitude() != null) {
            validateCoordinate(request.getLatitude(), BigDecimal.valueOf(-90), BigDecimal.valueOf(90), "latitude");
        }
        if (request.getLongitude() != null) {
            validateCoordinate(request.getLongitude(), BigDecimal.valueOf(-180), BigDecimal.valueOf(180), "longitude");
        }
    }

    private void validateCoordinate(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw badRequest(fieldName + "超出有效范围");
        }
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
            if (existing != null && !shouldAttemptSendFromExistingReminder(existing)) {
                return;
            }

            User user = userMapper.selectById(userId);
            String openid = user == null ? null : normalizeOptional(user.getOpenid());

            RecordReminder reminder = existing == null ? new RecordReminder() : existing;
            if (existing == null) {
                reminder.setRecordId(recordId);
                reminder.setUserId(userId);
                reminder.setTemplateType(TEMPLATE_TYPE_UNLOCK_REMINDER);
                reminder.setCreatedAt(now);
            }
            reminder.setUpdatedAt(now);
            if (openid == null) {
                reminder.setReminderStatus(RecordReminderStatus.SKIPPED_NO_OPENID);
                reminder.setLastError(OPENID_NOT_BOUND_MESSAGE);
            } else if (!appWechatProperties.hasUnlockReminderTemplate()) {
                reminder.setReminderStatus(RecordReminderStatus.NOT_CONFIGURED);
                reminder.setLastError(TEMPLATE_NOT_CONFIGURED_MESSAGE);
            } else if (existing == null) {
                reminder.setReminderStatus(RecordReminderStatus.REQUESTED);
                reminder.setLastError(null);
            } else {
                reminder.setReminderStatus(RecordReminderStatus.SEND_PENDING);
                reminder.setLastError(null);
            }

            if (existing == null) {
                recordReminderMapper.insert(reminder);
            } else {
                recordReminderMapper.updateStatusById(
                        reminder.getId(),
                        reminder.getReminderStatus(),
                        reminder.getLastError(),
                        null,
                        now);
            }
            if (reminder.getReminderStatus() == RecordReminderStatus.SEND_PENDING) {
                sendUnlockReminder(reminder, openid, now);
            }
        } catch (Exception ex) {
            // Reminder persistence is best-effort in M2 and must never block unlock
            // processing.
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

    private void ensureEditable(Record record, String message) {
        if (record.getStatus() == RecordStatus.SAVED) {
            return;
        }
        if (record.getStatus() != RecordStatus.DRAFT) {
            throw badRequest(message);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (record.getDraftExpiresAt() != null && !record.getDraftExpiresAt().isAfter(now)) {
            throw badRequest("草稿已过期，无法编辑");
        }
    }

    @Override
    @Transactional
    public RecordDetailVO save(Long userId, Long id) {
        assertOwnershipWritable(userId);
        Record current = requireOwnedRecord(id, userId);
        if (current.getStatus() == RecordStatus.SAVED) {
            return toDetailVO(current);
        }
        if (current.getStatus() != RecordStatus.DRAFT) {
            throw badRequest("仅DRAFT状态允许保存");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (current.getDraftExpiresAt() == null || !current.getDraftExpiresAt().isAfter(now)) {
            throw badRequest("草稿已过期，无法保存");
        }
        if (!recordSaveEligibility.isEligible(current)) {
            throw badRequest("至少留下一句话、一张图片或一段声音");
        }

        int affected = recordMapper.saveDraftByIdAndUserId(id, userId, now);
        if (affected == 0) {
            throw badRequest("记录状态已变更，请刷新后重试");
        }
        return toDetailVO(requireOwnedRecord(id, userId));
    }

    private String normalizeContent(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "" : normalized;
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
        vo.setCover(toCoverVO(record));
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
        item.setCover(toCoverVO(record));
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
        vo.setRealityLaterSubmitCount(laterReflectionSubmitCount(record));
        vo.setLifeNodeType(record.getLifeNodeType());
        vo.setLifeNodeCustomLabel(record.getLifeNodeCustomLabel());
        vo.setLifeNodeLabel(resolveLifeNodeLabel(record));
        vo.setLocation(
                toLocationVO(recordLocationMapper.selectByRecordIdAndUserId(record.getId(), record.getUserId())));
        List<RecordAttachmentVO> attachments = loadAvailableAttachments(record.getId(), record.getUserId());
        vo.setAttachments(attachments);
        vo.setCover(toCoverVO(record));
        vo.setUnlockReminderStatus(resolveUnlockReminderStatus(record.getId()));
        vo.setTags(loadRecordTags(record.getId()));
        boolean hasReply = record.getStatus() == RecordStatus.UNLOCKED
                && replyMapper.selectByRecordId(record.getId()) != null;
        vo.setHasReply(hasReply);
        vo.setCanReply(record.getStatus() == RecordStatus.UNLOCKED && !hasReply);
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private List<RecordAttachmentVO> loadAvailableAttachments(Long recordId, Long userId) {
        List<RecordAttachment> attachments = recordAttachmentMapper.selectAvailableByRecordIdAndUserId(recordId,
                userId);
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream().map(this::toAttachmentVO).toList();
    }

    private RecordAttachmentVO toCoverVO(Record record) {
        Long coverAttachmentId = record.getCoverAttachmentId();
        if (coverAttachmentId == null) {
            return null;
        }
        RecordAttachment attachment = recordAttachmentMapper.selectByIdAndRecordIdAndUserId(
                coverAttachmentId,
                record.getId(),
                record.getUserId());
        if (attachment == null
                || attachment.getStatus() != RecordAttachmentStatus.AVAILABLE
                || attachment.getType() != RecordAttachmentType.IMAGE) {
            return null;
        }
        return toAttachmentVO(attachment);
    }

    private RecordAttachmentVO toAttachmentVO(RecordAttachment attachment) {
        RecordAttachmentVO vo = new RecordAttachmentVO();
        vo.setId(attachment.getId());
        vo.setRecordId(attachment.getRecordId());
        vo.setType(attachment.getType());
        vo.setStatus(attachment.getStatus());
        vo.setFileName(attachment.getFileName());
        vo.setMimeType(attachment.getMimeType());
        vo.setSizeBytes(attachment.getSizeBytes());
        vo.setWidth(attachment.getWidth());
        vo.setHeight(attachment.getHeight());
        vo.setDurationSeconds(attachment.getDurationSeconds());
        vo.setSortOrder(attachment.getSortOrder());
        vo.setCreatedAt(attachment.getCreatedAt());
        vo.setAccessUrl(null);
        return vo;
    }

    private RecordLocationVO toLocationVO(RecordLocation location) {
        if (location == null) {
            return null;
        }
        RecordLocationVO vo = new RecordLocationVO();
        vo.setSource(location.getSource());
        vo.setName(location.getName());
        vo.setAddress(location.getAddress());
        vo.setLatitude(location.getLatitude());
        vo.setLongitude(location.getLongitude());
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

    private boolean isAuthorizationStatus(RecordReminderStatus status) {
        return status == RecordReminderStatus.REQUESTED
                || status == RecordReminderStatus.AUTHORIZED
                || status == RecordReminderStatus.DENIED;
    }

    private String authorizationLastError(RecordReminderStatus status) {
        if (status == RecordReminderStatus.REQUESTED) {
            return AUTHORIZATION_UNAVAILABLE_MESSAGE;
        }
        if (status == RecordReminderStatus.DENIED) {
            return AUTHORIZATION_DENIED_MESSAGE;
        }
        return null;
    }

    private boolean shouldAttemptSendFromExistingReminder(RecordReminder existing) {
        RecordReminderStatus status = existing.getReminderStatus();
        return status == RecordReminderStatus.AUTHORIZED
                || status == RecordReminderStatus.SEND_FAILED;
    }

    private RecordReminderStatus resolveUnlockReminderStatus(Long recordId) {
        RecordReminder reminder = recordReminderMapper.selectByRecordIdAndTemplateType(
                recordId,
                TEMPLATE_TYPE_UNLOCK_REMINDER);
        return reminder == null ? null : reminder.getReminderStatus();
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
