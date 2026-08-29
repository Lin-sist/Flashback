package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.common.page.PageResult;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.TimeChapter;
import com.flashback.domain.TimeChapterRecord;
import com.flashback.domain.TimeChapterStatus;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.CreateTimeChapterRequest;
import com.flashback.dto.TimeChapterMemberPageQuery;
import com.flashback.dto.TimeChapterPageQuery;
import com.flashback.dto.TransferConfirmation;
import com.flashback.dto.UpdateTimeChapterRequest;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.TimeChapterMapper;
import com.flashback.mapper.TimeChapterRecordMapper;
import com.flashback.service.TimeChapterService;
import com.flashback.service.data.DataOwnershipMutationGuard;
import com.flashback.vo.RecordAttachmentVO;
import com.flashback.vo.RecordChapterSummaryVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.TimeChapterDetailVO;
import com.flashback.vo.TimeChapterSummaryVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 时间篇章的唯一业务入口。
 *
 * 所有写操作先通过数据所有权冻结，再在一个事务中锁定 chapter / record 并完成关系变更；
 * 该服务不触碰记录正文、位置、附件、封存或 Agent 字段。
 */
@Service
public class TimeChapterServiceImpl implements TimeChapterService {

    private static final int MAX_BATCH = 100;
    private final TimeChapterMapper chapterMapper;
    private final TimeChapterRecordMapper relationMapper;
    private final RecordMapper recordMapper;
    private final TagMapper tagMapper;
    private final RecordAttachmentMapper attachmentMapper;
    private final DataOwnershipMutationGuard mutationGuard;
    private final Clock clock;

    public TimeChapterServiceImpl(TimeChapterMapper chapterMapper,
            TimeChapterRecordMapper relationMapper,
            RecordMapper recordMapper,
            TagMapper tagMapper,
            RecordAttachmentMapper attachmentMapper,
            DataOwnershipMutationGuard mutationGuard,
            Clock clock) {
        this.chapterMapper = chapterMapper;
        this.relationMapper = relationMapper;
        this.recordMapper = recordMapper;
        this.tagMapper = tagMapper;
        this.attachmentMapper = attachmentMapper;
        this.mutationGuard = mutationGuard;
        this.clock = clock;
    }

    @Override
    public PageResult<TimeChapterSummaryVO> page(Long userId, TimeChapterPageQuery query) {
        TimeChapterPageQuery actual = query == null ? new TimeChapterPageQuery() : query;
        int pageNum = actual.getPageNum();
        int pageSize = actual.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        long total = chapterMapper.countByUserAndStatus(userId, actual.getStatus());
        List<TimeChapter> chapters = total == 0
                ? List.of()
                : chapterMapper.selectPageByUserAndStatus(userId, actual.getStatus(), offset, pageSize);
        return PageResult.of(chapters.stream().map(this::toSummary).toList(), total, pageNum, pageSize);
    }

    @Override
    public TimeChapterDetailVO detail(Long userId, Long chapterId, TimeChapterMemberPageQuery query) {
        TimeChapterMemberPageQuery actual = query == null ? new TimeChapterMemberPageQuery() : query;
        String order = normalizeOrder(actual.getOrder());
        TimeChapter chapter = requireChapter(userId, chapterId);
        int pageNum = actual.getPageNum();
        int pageSize = actual.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        long total = relationMapper.countByChapterIdAndUserId(chapterId, userId);
        List<Record> records = total == 0
                ? List.of()
                : relationMapper.selectMemberRecords(chapterId, userId, order, offset, pageSize);
        RecordChapterSummaryVO chapterSummary = toRecordChapterSummary(chapter);
        Map<Long, List<String>> tagNames = loadTagNames(records);
        List<RecordListItemVO> members = records.stream()
                .map(record -> toListItem(record, tagNames.getOrDefault(record.getId(), List.of()), chapterSummary))
                .toList();

        TimeChapterDetailVO vo = new TimeChapterDetailVO();
        copySummary(chapter, vo);
        vo.setMembers(PageResult.of(members, total, pageNum, pageSize));
        return vo;
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO create(Long userId, CreateTimeChapterRequest request) {
        assertWritable(userId);
        if (request == null) throw bad("创建篇章请求不能为空");
        String name = normalizeRequired(request.getName(), "name不能为空");
        String note = normalizeNote(request.getNote());
        List<Long> recordIds = normalizeRecordIds(request.getRecordIds());
        Map<Long, TimeChapterRecord> relationSnapshot = readRelations(userId, recordIds);
        List<Long> sourceIds = sourceChapterIds(relationSnapshot, null);
        Map<Long, TimeChapter> sources = lockChapters(userId, sourceIds);
        Map<Long, Record> records = lockOwnedRecords(userId, recordIds);
        validateChapterEligible(recordIds, records);
        Map<Long, TimeChapterRecord> currentRelations = lockRelations(userId, recordIds);
        assertRelationSnapshotCurrent(relationSnapshot, currentRelations);
        Map<Long, TransferConfirmation> transfers = transferMap(request.getTransfers(), recordIds);
        validateTransfersForCreate(recordIds, currentRelations, transfers, sources);

        LocalDateTime now = now();
        TimeChapter chapter = new TimeChapter();
        chapter.setUserId(userId);
        chapter.setName(name);
        chapter.setNote(note);
        chapter.setStatus(TimeChapterStatus.ACTIVE);
        chapter.setVersion(0L);
        chapter.setCreatedAt(now);
        chapter.setUpdatedAt(now);
        chapterMapper.insert(chapter);

        for (Long recordId : recordIds) {
            TimeChapterRecord existing = currentRelations.get(recordId);
            if (existing != null) {
                relationMapper.deleteByChapterIdAndRecordIdAndUserId(existing.getChapterId(), recordId, userId);
            }
            insertRelation(chapter.getId(), recordId, userId, now);
        }
        bumpSources(sources.keySet(), userId, now);
        return toSummary(requireChapter(userId, chapter.getId()));
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO update(Long userId, Long chapterId, UpdateTimeChapterRequest request) {
        assertWritable(userId);
        if (request == null) throw bad("修改篇章请求不能为空");
        TimeChapter current = lockChapter(userId, chapterId);
        assertVersion(current, request.getExpectedVersion());
        String name = normalizeRequired(request.getName(), "name不能为空");
        String note = normalizeNote(request.getNote());
        if (name.equals(current.getName()) && equalsNullable(note, current.getNote())) {
            return toSummary(requireChapter(userId, chapterId));
        }
        if (chapterMapper.updateMetadataIfVersion(chapterId, userId, name, note,
                request.getExpectedVersion(), now()) != 1) {
            throw conflict("篇章状态已变更，请刷新后重试");
        }
        return toSummary(requireChapter(userId, chapterId));
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO addMembers(Long userId, Long chapterId, ChangeTimeChapterMembersRequest request) {
        assertWritable(userId);
        if (request == null) throw bad("加入篇章请求不能为空");
        List<Long> recordIds = normalizeRecordIds(request.getRecordIds());
        Map<Long, TimeChapterRecord> relationSnapshot = readRelations(userId, recordIds);
        List<Long> plannedChapterIds = new ArrayList<>(sourceChapterIds(relationSnapshot, null));
        plannedChapterIds.add(chapterId);
        Map<Long, TimeChapter> lockedChapters = lockChapters(userId, plannedChapterIds.stream().distinct().sorted().toList());
        TimeChapter target = lockedChapters.get(chapterId);
        if (target == null) throw new NotFoundException("篇章不存在");
        if (target.getStatus() != TimeChapterStatus.ACTIVE) throw bad("已结束篇章不能加入记录");
        Map<Long, Record> records = lockOwnedRecords(userId, recordIds);
        validateChapterEligible(recordIds, records);
        Map<Long, TimeChapterRecord> currentRelations = lockRelations(userId, recordIds);
        assertRelationSnapshotCurrent(relationSnapshot, currentRelations);
        Map<Long, TransferConfirmation> transfers = transferMap(request.getTransfers(), recordIds);
        List<Long> sourceIds = sourceChapterIds(currentRelations, chapterId);

        boolean hasChange = false;
        for (Long recordId : recordIds) {
            TimeChapterRecord existing = currentRelations.get(recordId);
            if (existing == null) {
                if (transfers.containsKey(recordId)) throw conflict("篇章归属已变更，请刷新后重试");
                hasChange = true;
            } else if (!chapterId.equals(existing.getChapterId())) {
                TransferConfirmation transfer = transfers.get(recordId);
                if (transfer == null || !existing.getChapterId().equals(transfer.getFromChapterId())) {
                    throw conflict("篇章归属已变更，请刷新后重试");
                }
                hasChange = true;
            } else if (transfers.containsKey(recordId)) {
                throw bad("记录已经属于目标篇章");
            }
        }
        if (!hasChange) return toSummary(requireChapter(userId, chapterId));
        assertVersion(target, request.getExpectedVersion());

        LocalDateTime now = now();
        for (Long recordId : recordIds) {
            TimeChapterRecord existing = currentRelations.get(recordId);
            if (existing != null && !chapterId.equals(existing.getChapterId())) {
                relationMapper.deleteByChapterIdAndRecordIdAndUserId(existing.getChapterId(), recordId, userId);
            }
            if (existing == null || !chapterId.equals(existing.getChapterId())) {
                insertRelation(chapterId, recordId, userId, now);
            }
        }
        bumpSources(Set.copyOf(sourceIds), userId, now);
        chapterMapper.bumpVersion(chapterId, userId, now);
        return toSummary(requireChapter(userId, chapterId));
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO removeMembers(Long userId, Long chapterId, ChangeTimeChapterMembersRequest request) {
        assertWritable(userId);
        if (request == null) throw bad("移出篇章请求不能为空");
        TimeChapter target = lockChapter(userId, chapterId);
        List<Long> recordIds = normalizeRecordIds(request.getRecordIds());
        Map<Long, Record> records = lockOwnedRecords(userId, recordIds);
        validateChapterEligible(recordIds, records);
        Map<Long, TimeChapterRecord> currentRelations = lockRelations(userId, recordIds);
        boolean hasChange = recordIds.stream().anyMatch(id -> {
            TimeChapterRecord relation = currentRelations.get(id);
            return relation != null && chapterId.equals(relation.getChapterId());
        });
        if (!hasChange) return toSummary(requireChapter(userId, chapterId));
        assertVersion(target, request.getExpectedVersion());
        for (Long recordId : recordIds) {
            TimeChapterRecord relation = currentRelations.get(recordId);
            if (relation != null && chapterId.equals(relation.getChapterId())) {
                relationMapper.deleteByChapterIdAndRecordIdAndUserId(chapterId, recordId, userId);
            }
        }
        chapterMapper.bumpVersion(chapterId, userId, now());
        return toSummary(requireChapter(userId, chapterId));
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO end(Long userId, Long chapterId, Long expectedVersion) {
        return changeLifecycle(userId, chapterId, expectedVersion, TimeChapterStatus.ENDED, now());
    }

    @Override
    @Transactional
    public TimeChapterSummaryVO reopen(Long userId, Long chapterId, Long expectedVersion) {
        return changeLifecycle(userId, chapterId, expectedVersion, TimeChapterStatus.ACTIVE, null);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long chapterId, Long expectedVersion) {
        assertWritable(userId);
        TimeChapter current = lockChapter(userId, chapterId);
        assertVersion(current, expectedVersion);
        if (chapterMapper.deleteByIdAndUserIdAndVersion(chapterId, userId, expectedVersion) != 1) {
            throw conflict("篇章状态已变更，请刷新后重试");
        }
    }

    private TimeChapterSummaryVO changeLifecycle(Long userId, Long chapterId, Long expectedVersion,
            TimeChapterStatus targetStatus, LocalDateTime endedAt) {
        assertWritable(userId);
        TimeChapter current = lockChapter(userId, chapterId);
        if (current.getStatus() == targetStatus) return toSummary(requireChapter(userId, chapterId));
        assertVersion(current, expectedVersion);
        if (chapterMapper.updateLifecycleIfVersion(chapterId, userId, targetStatus, endedAt,
                expectedVersion, now()) != 1) {
            throw conflict("篇章状态已变更，请刷新后重试");
        }
        return toSummary(requireChapter(userId, chapterId));
    }

    private TimeChapter requireChapter(Long userId, Long chapterId) {
        TimeChapter chapter = chapterMapper.selectByIdAndUserId(chapterId, userId);
        if (chapter == null) throw new NotFoundException("篇章不存在");
        assertInvariant(chapter);
        return chapter;
    }

    private TimeChapter lockChapter(Long userId, Long chapterId) {
        TimeChapter chapter = chapterMapper.selectByIdAndUserIdForUpdate(chapterId, userId);
        if (chapter == null) throw new NotFoundException("篇章不存在");
        assertInvariant(chapter);
        return chapter;
    }

    private void assertInvariant(TimeChapter chapter) {
        boolean valid = (chapter.getStatus() == TimeChapterStatus.ACTIVE && chapter.getEndedAt() == null)
                || (chapter.getStatus() == TimeChapterStatus.ENDED && chapter.getEndedAt() != null);
        if (!valid) throw conflict("篇章状态数据无效，请联系管理员");
    }

    private Map<Long, Record> lockOwnedRecords(Long userId, List<Long> recordIds) {
        Map<Long, Record> result = new LinkedHashMap<>();
        for (Long recordId : recordIds) {
            Record record = recordMapper.selectByIdAndUserIdForChapterUpdate(recordId, userId);
            if (record == null) throw new NotFoundException("记录不存在");
            result.put(recordId, record);
        }
        return result;
    }

    private Map<Long, TimeChapterRecord> lockRelations(Long userId, List<Long> recordIds) {
        Map<Long, TimeChapterRecord> result = new LinkedHashMap<>();
        for (Long recordId : recordIds) {
            TimeChapterRecord relation = relationMapper.selectByRecordIdAndUserIdForUpdate(recordId, userId);
            if (relation != null) result.put(recordId, relation);
        }
        return result;
    }

    private Map<Long, TimeChapterRecord> readRelations(Long userId, List<Long> recordIds) {
        Map<Long, TimeChapterRecord> result = new LinkedHashMap<>();
        for (TimeChapterRecord relation : relationMapper.selectByRecordIdsAndUserId(recordIds, userId)) {
            result.put(relation.getRecordId(), relation);
        }
        return result;
    }

    private void assertRelationSnapshotCurrent(Map<Long, TimeChapterRecord> snapshot,
            Map<Long, TimeChapterRecord> current) {
        if (snapshot.size() != current.size()) {
            throw conflict("篇章归属已变更，请刷新后重试");
        }
        for (Map.Entry<Long, TimeChapterRecord> entry : snapshot.entrySet()) {
            TimeChapterRecord latest = current.get(entry.getKey());
            if (latest == null || !entry.getValue().getChapterId().equals(latest.getChapterId())) {
                throw conflict("篇章归属已变更，请刷新后重试");
            }
        }
    }

    private Map<Long, TimeChapter> lockChapters(Long userId, List<Long> chapterIds) {
        Map<Long, TimeChapter> result = new LinkedHashMap<>();
        for (Long chapterId : chapterIds) {
            TimeChapter chapter = lockChapter(userId, chapterId);
            result.put(chapterId, chapter);
        }
        return result;
    }

    private List<Long> sourceChapterIds(Map<Long, TimeChapterRecord> relations, Long targetChapterId) {
        return relations.values().stream()
                .map(TimeChapterRecord::getChapterId)
                .filter(id -> targetChapterId == null || !targetChapterId.equals(id))
                .distinct()
                .sorted()
                .toList();
    }

    private void validateChapterEligible(List<Long> recordIds, Map<Long, Record> records) {
        if (records.size() != recordIds.size()) throw new NotFoundException("记录不存在");
        for (Long recordId : recordIds) {
            if (records.get(recordId).getStatus() == RecordStatus.DRAFT) {
                throw bad("草稿不能加入篇章");
            }
        }
    }

    private void validateTransfersForCreate(List<Long> recordIds, Map<Long, TimeChapterRecord> relations,
            Map<Long, TransferConfirmation> transfers, Map<Long, TimeChapter> sources) {
        for (Long recordId : recordIds) {
            TimeChapterRecord relation = relations.get(recordId);
            TransferConfirmation transfer = transfers.get(recordId);
            if (relation == null) {
                if (transfer != null) throw conflict("篇章归属已变更，请刷新后重试");
            } else if (transfer == null || !relation.getChapterId().equals(transfer.getFromChapterId())) {
                throw conflict("篇章归属已变更，请刷新后重试");
            }
        }
        if (sources.size() != sourceChapterIds(relations, null).size()) {
            throw new NotFoundException("篇章不存在");
        }
    }

    private void bumpSources(Set<Long> sourceIds, Long userId, LocalDateTime now) {
        for (Long sourceId : sourceIds) chapterMapper.bumpVersion(sourceId, userId, now);
    }

    private void insertRelation(Long chapterId, Long recordId, Long userId, LocalDateTime now) {
        TimeChapterRecord relation = new TimeChapterRecord();
        relation.setChapterId(chapterId);
        relation.setRecordId(recordId);
        relation.setUserId(userId);
        relation.setAddedAt(now);
        relationMapper.insert(relation);
    }

    private List<Long> normalizeRecordIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) throw bad("recordIds不能为空");
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long recordId : recordIds) {
            if (recordId == null || recordId <= 0) throw bad("recordIds存在无效值");
            unique.add(recordId);
        }
        if (unique.isEmpty()) throw bad("recordIds不能为空");
        if (unique.size() > MAX_BATCH) throw bad("单次最多处理100条记录");
        return unique.stream().sorted().toList();
    }

    private Map<Long, TransferConfirmation> transferMap(List<TransferConfirmation> transfers, List<Long> recordIds) {
        if (transfers == null || transfers.isEmpty()) return Map.of();
        Set<Long> allowed = Set.copyOf(recordIds);
        Map<Long, TransferConfirmation> result = new LinkedHashMap<>();
        for (TransferConfirmation transfer : transfers) {
            if (transfer == null || transfer.getRecordId() == null || transfer.getFromChapterId() == null
                    || transfer.getFromChapterId() <= 0 || !allowed.contains(transfer.getRecordId())) {
                throw bad("transfers与recordIds不匹配");
            }
            if (result.put(transfer.getRecordId(), transfer) != null) throw bad("transfers不能重复");
        }
        return result;
    }

    private Map<Long, List<String>> loadTagNames(List<Record> records) {
        if (records.isEmpty()) return Map.of();
        Map<Long, List<String>> result = new LinkedHashMap<>();
        tagMapper.selectTagNamesByRecordIds(records.stream().map(Record::getId).toList())
                .forEach(row -> result.computeIfAbsent(row.getRecordId(), key -> new ArrayList<>()).add(row.getTagName()));
        return result;
    }

    private RecordListItemVO toListItem(Record record, List<String> tagNames, RecordChapterSummaryVO chapter) {
        RecordListItemVO vo = new RecordListItemVO();
        vo.setId(record.getId());
        vo.setTitle(record.getTitle());
        vo.setContentPreview(toPreview(record.getContent()));
        vo.setRecordType(record.getRecordType());
        vo.setStatus(record.getStatus());
        vo.setUnlockAt(record.getUnlockAt());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setTagNames(tagNames);
        vo.setChapter(chapter);
        vo.setCover(toCover(record));
        return vo;
    }

    private RecordAttachmentVO toCover(Record record) {
        if (record.getCoverAttachmentId() == null) return null;
        RecordAttachment attachment = attachmentMapper.selectByIdAndRecordIdAndUserId(
                record.getCoverAttachmentId(), record.getId(), record.getUserId());
        if (attachment == null || attachment.getStatus() != RecordAttachmentStatus.AVAILABLE
                || attachment.getType() != RecordAttachmentType.IMAGE) return null;
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
        return vo;
    }

    private String toPreview(String content) {
        if (content == null || content.isBlank()) return "";
        String normalized = content.trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "...";
    }

    private TimeChapterSummaryVO toSummary(TimeChapter chapter) {
        TimeChapterSummaryVO vo = new TimeChapterSummaryVO();
        copySummary(chapter, vo);
        return vo;
    }

    private void copySummary(TimeChapter chapter, TimeChapterSummaryVO vo) {
        vo.setId(chapter.getId());
        vo.setName(chapter.getName());
        vo.setNote(chapter.getNote());
        vo.setStatus(chapter.getStatus());
        vo.setMemberCount(chapter.getMemberCount() == null ? 0 : chapter.getMemberCount());
        vo.setCoverageStartAt(chapter.getCoverageStartAt());
        vo.setCoverageEndAt(chapter.getCoverageEndAt());
        vo.setEndedAt(chapter.getEndedAt());
        vo.setVersion(chapter.getVersion());
        vo.setCreatedAt(chapter.getCreatedAt());
        vo.setUpdatedAt(chapter.getUpdatedAt());
    }

    private RecordChapterSummaryVO toRecordChapterSummary(TimeChapter chapter) {
        RecordChapterSummaryVO vo = new RecordChapterSummaryVO();
        vo.setId(chapter.getId());
        vo.setName(chapter.getName());
        vo.setStatus(chapter.getStatus());
        return vo;
    }

    private String normalizeOrder(String raw) {
        if (raw == null || raw.isBlank()) return "DESC";
        String order = raw.trim().toUpperCase();
        if (!"ASC".equals(order) && !"DESC".equals(order)) throw bad("order仅支持ASC或DESC");
        return order;
    }

    private String normalizeRequired(String raw, String message) {
        String value = raw == null ? null : raw.trim();
        if (value == null || value.isEmpty()) throw bad(message);
        if (value.length() > 100) throw bad("name长度不能超过100");
        return value;
    }

    private String normalizeNote(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() > 1000) throw bad("note长度不能超过1000");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) throw bad("note不能包含控制字符");
        }
        return value;
    }

    private void assertVersion(TimeChapter chapter, Long expectedVersion) {
        if (expectedVersion == null || expectedVersion < 0 || !expectedVersion.equals(chapter.getVersion())) {
            throw conflict("篇章状态已变更，请刷新后重试");
        }
    }

    private void assertWritable(Long userId) { mutationGuard.assertWritable(userId); }

    private boolean equalsNullable(Object left, Object right) { return left == null ? right == null : left.equals(right); }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    private BizException bad(String message) { return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message); }

    private BizException conflict(String message) { return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, message); }
}
