package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.config.AppDataOwnershipProperties;
import com.flashback.domain.*;
import com.flashback.domain.Record;
import com.flashback.mapper.*;
import com.flashback.service.DataOwnershipService;
import com.flashback.service.data.*;
import com.flashback.storage.ObjectStorageException;
import com.flashback.vo.DataOperationVO;
import com.flashback.vo.DataOwnershipSummaryVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DataOwnershipServiceImpl implements DataOwnershipService {
    private static final List<DataOperationType> ALL_TYPES = List.of(DataOperationType.EXPORT, DataOperationType.DELETE_RECORD, DataOperationType.CLEAR_ALL_RECORDS);
    private final DataOperationMapper operationMapper;
    private final DataOperationRecordMapper itemMapper;
    private final RecordMapper recordMapper;
    private final RecordAttachmentMapper attachmentMapper;
    private final RecordLocationMapper locationMapper;
    private final TagMapper tagMapper;
    private final ReplyMapper replyMapper;
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final PrivateObjectContentReader objectReader;
    private final DataExportPackageBuilder packageBuilder;
    private final DataOwnershipArtifactStore artifactStore;
    private final DataDeletionWorker deletionWorker;
    private final AppDataOwnershipProperties properties;
    private final Clock clock;

    public DataOwnershipServiceImpl(DataOperationMapper operationMapper, DataOperationRecordMapper itemMapper,
            RecordMapper recordMapper, RecordAttachmentMapper attachmentMapper, RecordLocationMapper locationMapper,
            TagMapper tagMapper, ReplyMapper replyMapper, AgentSessionMapper agentSessionMapper,
            AgentMessageMapper agentMessageMapper, PrivateObjectContentReader objectReader,
            DataExportPackageBuilder packageBuilder, DataOwnershipArtifactStore artifactStore,
            DataDeletionWorker deletionWorker, AppDataOwnershipProperties properties, Clock clock) {
        this.operationMapper = operationMapper; this.itemMapper = itemMapper; this.recordMapper = recordMapper;
        this.attachmentMapper = attachmentMapper; this.locationMapper = locationMapper; this.tagMapper = tagMapper;
        this.replyMapper = replyMapper; this.agentSessionMapper = agentSessionMapper; this.agentMessageMapper = agentMessageMapper;
        this.objectReader = objectReader; this.packageBuilder = packageBuilder; this.artifactStore = artifactStore;
        this.deletionWorker = deletionWorker; this.properties = properties; this.clock = clock;
    }

    @Override
    public DataOwnershipSummaryVO summary(Long userId) {
        List<Record> records = recordMapper.selectAllByUserId(userId);
        EnumMap<RecordStatus, Long> counts = new EnumMap<>(RecordStatus.class);
        for (RecordStatus status : RecordStatus.values()) counts.put(status, 0L);
        records.forEach(r -> counts.compute(r.getStatus(), (k, v) -> v + 1));
        DataOwnershipSummaryVO vo = new DataOwnershipSummaryVO();
        counts.forEach((k, v) -> vo.getRecordCounts().put(k.name(), v));
        Long mediaBytes = attachmentMapper.sumAvailableSizeByUserId(userId);
        vo.setMediaBytes(mediaBytes == null ? 0 : mediaBytes);
        DataOperation active = operationMapper.selectLatestActiveByUser(userId);
        if (active != null) vo.setActiveOperation(toVO(active, null));
        return vo;
    }

    @Override
    public DataOperationVO createExport(Long userId, SealedContentPolicy policy) {
        if (policy == null) throw bad("必须选择封存内容导出策略");
        assertNoConflict(userId);
        LocalDateTime now = now();
        DataOperation op = newOperation(userId, DataOperationType.EXPORT, DataOperationStatus.PENDING, now);
        op.setSealedContentPolicy(policy);
        op.setTotalItems(recordMapper.selectAllByUserId(userId).size());
        operationMapper.insert(op);
        processExport(op);
        return getOperation(userId, op.getId());
    }

    @Override
    public DataOperationVO getOperation(Long userId, Long operationId) {
        DataOperation op = requireOperation(userId, operationId);
        if (op.getStatus() == DataOperationStatus.PREPARED && expired(op.getConfirmationExpiresAt())) expire(op, DataOperationStatus.PREPARED);
        if (op.getStatus() == DataOperationStatus.SUCCEEDED && op.getOperationType() == DataOperationType.EXPORT && expired(op.getArtifactExpiresAt())) {
            artifactStore.delete(op.getArtifactToken()); expire(op, DataOperationStatus.SUCCEEDED);
        }
        return toVO(requireOperation(userId, operationId), null);
    }

    @Override
    public byte[] downloadExport(Long userId, Long operationId) {
        DataOperation op = requireOperation(userId, operationId);
        if (op.getOperationType() != DataOperationType.EXPORT || op.getStatus() != DataOperationStatus.SUCCEEDED) throw conflict("导出包尚未可下载");
        if (expired(op.getArtifactExpiresAt())) { artifactStore.delete(op.getArtifactToken()); expire(op, DataOperationStatus.SUCCEEDED); throw gone("导出包已过期，请重新导出"); }
        try { return artifactStore.read(op.getArtifactToken()); }
        catch (Exception ex) { throw new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, "导出包暂时不可读取，请稍后重试"); }
    }

    @Override
    public DataOperationVO prepareDeletion(Long userId, DataDeletionScope scope, Long recordId) {
        if (scope == null) throw bad("删除范围不能为空");
        if ((scope == DataDeletionScope.RECORD) != (recordId != null)) throw bad("单条删除必须且只能指定 recordId");
        assertNoConflict(userId);
        List<Record> records;
        DataOperationType type;
        if (scope == DataDeletionScope.RECORD) {
            Record record = recordMapper.selectByIdAndUserIdForDeletion(recordId, userId);
            if (record == null) throw notFound("记录不存在");
            records = List.of(record); type = DataOperationType.DELETE_RECORD;
        } else {
            records = recordMapper.selectAllByUserId(userId); type = DataOperationType.CLEAR_ALL_RECORDS;
        }
        LocalDateTime now = now();
        String phrase = (scope == DataDeletionScope.RECORD ? "删除这条记录 " : "清除全部记录 ")
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        DataOperation op = newOperation(userId, type, DataOperationStatus.PREPARED, now);
        op.setTotalItems(records.size()); op.setConfirmationNonceHash(hash(phrase));
        op.setConfirmationExpiresAt(now.plusMinutes(properties.getConfirmationTtlMinutes()));
        operationMapper.insert(op);
        for (Record record : records) itemMapper.insert(newItem(op, record.getId(), now));
        return toVO(op, phrase);
    }

    @Override
    public DataOperationVO confirmDeletion(Long userId, Long intentId, String confirmationText) {
        DataOperation op = requireOperation(userId, intentId);
        if (op.getOperationType() == DataOperationType.EXPORT) throw bad("该操作不是删除 intent");
        if (op.getStatus() != DataOperationStatus.PREPARED) return toVO(op, null);
        if (expired(op.getConfirmationExpiresAt())) { expire(op, DataOperationStatus.PREPARED); throw gone("确认已过期，请重新发起"); }
        if (!MessageDigest.isEqual(op.getConfirmationNonceHash().getBytes(StandardCharsets.UTF_8), hash(confirmationText).getBytes(StandardCharsets.UTF_8))) throw bad("确认短语不匹配");
        LocalDateTime now = now(); DataOperationStatus expected = op.getStatus();
        op.setStatus(DataOperationStatus.PENDING); op.setConfirmedAt(now); op.setUpdatedAt(now);
        if (operationMapper.updateIfStatus(op, expected) != 1) return getOperation(userId, intentId);
        processDeletion(op);
        return getOperation(userId, intentId);
    }

    @Override
    public DataOperationVO retry(Long userId, Long operationId) {
        DataOperation op = requireOperation(userId, operationId);
        if (op.getStatus() != DataOperationStatus.RETRY_REQUIRED) throw conflict("当前操作不可重试");
        if (op.getOperationType() == DataOperationType.EXPORT) processExport(op); else processDeletion(op);
        return getOperation(userId, operationId);
    }

    public void resumeStaleOperations() {
        for (DataOperation op : operationMapper.selectStaleRunnable(now().minusMinutes(15), 20)) {
            if (op.getStatus() == DataOperationStatus.RUNNING) {
                op.setStatus(DataOperationStatus.RETRY_REQUIRED); op.setFailureCode(DataOperationFailureCode.INVARIANT_VIOLATION); op.setUpdatedAt(now());
                if (operationMapper.updateIfStatus(op, DataOperationStatus.RUNNING) != 1) continue;
            }
            DataOperation current = requireOperation(op.getUserId(), op.getId());
            if (current.getOperationType() == DataOperationType.EXPORT) processExport(current); else processDeletion(current);
        }
    }

    private void processExport(DataOperation op) {
        if (!start(op, op.getStatus())) return;
        try {
            List<DataExportRecordSnapshot> snapshots = new ArrayList<>();
            for (Record record : recordMapper.selectAllByUserId(op.getUserId())) {
                boolean hidden = op.getSealedContentPolicy() == SealedContentPolicy.RESPECT_SEAL && record.getStatus() == RecordStatus.SEALED;
                List<DataExportRecordSnapshot.AttachmentContent> attachments = new ArrayList<>();
                List<DataExportRecordSnapshot.AgentConversation> conversations = new ArrayList<>();
                if (!hidden) {
                    for (RecordAttachment attachment : attachmentMapper.selectAvailableByRecordIdAndUserId(record.getId(), op.getUserId())) {
                        attachments.add(new DataExportRecordSnapshot.AttachmentContent(attachment, objectReader.read(attachment)));
                    }
                    for (AgentSession session : agentSessionMapper.selectByRecordIdAndUserId(record.getId(), op.getUserId())) {
                        conversations.add(new DataExportRecordSnapshot.AgentConversation(session, agentMessageMapper.selectBySessionId(session.getId())));
                    }
                }
                snapshots.add(new DataExportRecordSnapshot(record,
                        hidden ? null : locationMapper.selectByRecordIdAndUserId(record.getId(), op.getUserId()),
                        hidden ? List.of() : tagMapper.selectTagsByRecordId(record.getId()),
                        hidden ? null : replyMapper.selectByRecordId(record.getId()), attachments, conversations));
            }
            byte[] zip = packageBuilder.build(snapshots, op.getSealedContentPolicy());
            DataOwnershipArtifactStore.StoredArtifact artifact = artifactStore.save(zip);
            DataOperation current = requireOperation(op.getUserId(), op.getId());
            current.setStatus(DataOperationStatus.SUCCEEDED); current.setProcessedItems(current.getTotalItems()); current.setFailedItems(0);
            current.setArtifactToken(artifact.token()); current.setArtifactExpiresAt(artifact.expiresAt()); current.setFailureCode(null);
            current.setCompletedAt(now()); current.setUpdatedAt(now());
            operationMapper.updateIfStatus(current, DataOperationStatus.RUNNING);
        } catch (ObjectStorageException ex) { markRetry(op, DataOperationFailureCode.REMOTE_OBJECT_READ_FAILED); }
        catch (Exception ex) { markRetry(op, DataOperationFailureCode.ARTIFACT_BUILD_FAILED); }
    }

    private void processDeletion(DataOperation op) {
        if (!start(op, op.getStatus())) return;
        List<DataOperationRecord> items = itemMapper.selectByOperationIdAndUserId(op.getId(), op.getUserId());
        int succeeded = 0; int failed = 0; int permanentFailures = 0;
        DataOperationFailureCode aggregateFailure = null;
        for (DataOperationRecord item : items) {
            if (item.getItemStatus() == DataOperationItemStatus.SUCCEEDED) { succeeded++; continue; }
            if (item.getRecordId() == null) { markItemSucceeded(item); succeeded++; continue; }
            DataOperationItemStatus expected = item.getItemStatus();
            item.setItemStatus(DataOperationItemStatus.RUNNING); item.setAttemptCount(item.getAttemptCount() + 1); item.setFailureCode(null); item.setUpdatedAt(now());
            if (itemMapper.updateIfStatus(item, expected) != 1) { failed++; continue; }
            DataDeletionWorker.Result result = deletionWorker.deleteRecord(op.getUserId(), item.getRecordId());
            if (result.succeeded()) { markItemSucceeded(item); succeeded++; }
            else {
                item.setItemStatus(result.retryable() ? DataOperationItemStatus.RETRY_REQUIRED : DataOperationItemStatus.FAILED);
                item.setFailureCode(result.failureCode()); item.setUpdatedAt(now()); itemMapper.updateIfStatus(item, DataOperationItemStatus.RUNNING);
                aggregateFailure = result.failureCode(); failed++; if (!result.retryable()) permanentFailures++;
            }
        }
        DataOperation current = requireOperation(op.getUserId(), op.getId());
        current.setProcessedItems(succeeded); current.setFailedItems(failed); current.setUpdatedAt(now());
        current.setStatus(failed == 0 ? DataOperationStatus.SUCCEEDED : permanentFailures > 0 ? DataOperationStatus.FAILED : DataOperationStatus.RETRY_REQUIRED);
        current.setFailureCode(failed == 0 ? null : aggregateFailure);
        if (failed == 0) current.setCompletedAt(now());
        operationMapper.updateIfStatus(current, DataOperationStatus.RUNNING);
    }

    private void markItemSucceeded(DataOperationRecord item) {
        DataOperationItemStatus expected = item.getItemStatus(); item.setItemStatus(DataOperationItemStatus.SUCCEEDED); item.setFailureCode(null); item.setUpdatedAt(now());
        itemMapper.updateIfStatus(item, expected);
    }
    private boolean start(DataOperation op, DataOperationStatus expected) {
        DataOperation current = requireOperation(op.getUserId(), op.getId());
        if (current.getStatus() != expected || (expected != DataOperationStatus.PENDING && expected != DataOperationStatus.RETRY_REQUIRED)) return false;
        current.setStatus(DataOperationStatus.RUNNING); current.setStartedAt(current.getStartedAt() == null ? now() : current.getStartedAt()); current.setUpdatedAt(now()); current.setFailureCode(null);
        return operationMapper.updateIfStatus(current, expected) == 1;
    }
    private void markRetry(DataOperation op, DataOperationFailureCode code) {
        DataOperation current = requireOperation(op.getUserId(), op.getId());
        if (current.getStatus() != DataOperationStatus.RUNNING) return;
        current.setStatus(DataOperationStatus.RETRY_REQUIRED); current.setFailureCode(code); current.setFailedItems(Math.max(1, current.getFailedItems())); current.setUpdatedAt(now());
        operationMapper.updateIfStatus(current, DataOperationStatus.RUNNING);
    }
    private void expire(DataOperation op, DataOperationStatus expected) { op.setStatus(DataOperationStatus.EXPIRED); op.setFailureCode(DataOperationFailureCode.ARTIFACT_EXPIRED); op.setUpdatedAt(now()); operationMapper.updateIfStatus(op, expected); }
    private void assertNoConflict(Long userId) { if (operationMapper.countActiveByUserAndTypes(userId, ALL_TYPES) > 0) throw conflict("已有数据操作尚未结束"); }
    private DataOperation requireOperation(Long userId, Long id) { DataOperation op = operationMapper.selectByIdAndUserId(id, userId); if (op == null) throw notFound("数据操作不存在"); return op; }
    private DataOperation newOperation(Long userId, DataOperationType type, DataOperationStatus status, LocalDateTime now) { DataOperation op = new DataOperation(); op.setUserId(userId); op.setOperationType(type); op.setStatus(status); op.setCreatedAt(now); op.setUpdatedAt(now); return op; }
    private DataOperationRecord newItem(DataOperation op, Long recordId, LocalDateTime now) { DataOperationRecord item = new DataOperationRecord(); item.setOperationId(op.getId()); item.setUserId(op.getUserId()); item.setRecordId(recordId); item.setItemStatus(DataOperationItemStatus.PENDING); item.setCreatedAt(now); item.setUpdatedAt(now); return item; }
    private DataOperationVO toVO(DataOperation op, String phrase) { DataOperationVO vo = new DataOperationVO(); vo.setId(op.getId()); vo.setOperationType(op.getOperationType()); vo.setStatus(op.getStatus()); vo.setSealedContentPolicy(op.getSealedContentPolicy()); vo.setTotalItems(op.getTotalItems()); vo.setProcessedItems(op.getProcessedItems()); vo.setFailedItems(op.getFailedItems()); vo.setFailureCode(op.getFailureCode()); vo.setConfirmationExpiresAt(op.getConfirmationExpiresAt()); vo.setArtifactExpiresAt(op.getArtifactExpiresAt()); vo.setConfirmationText(phrase); vo.setRetryable(op.getStatus() == DataOperationStatus.RETRY_REQUIRED); vo.setDownloadable(op.getOperationType() == DataOperationType.EXPORT && op.getStatus() == DataOperationStatus.SUCCEEDED && !expired(op.getArtifactExpiresAt())); return vo; }
    private boolean expired(LocalDateTime value) { return value == null || !value.isAfter(now()); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private BizException bad(String message) { return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message); }
    private BizException conflict(String message) { return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, message); }
    private BizException notFound(String message) { return new BizException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, message); }
    private BizException gone(String message) { return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.GONE, message); }
}
