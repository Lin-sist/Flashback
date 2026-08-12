package com.flashback.mapper;

import com.flashback.domain.Record;
import com.flashback.domain.LifeNodeType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RecordMapper {

        int insert(Record record);

        Record selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

        Record selectByIdAndUserIdForDeletion(@Param("id") Long id, @Param("userId") Long userId);

        List<Record> selectAllByUserId(@Param("userId") Long userId);

        int deleteAnyByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

        int updateEditableByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("title") String title,
                        @Param("content") String content,
                        @Param("recordType") RecordType recordType,
                        @Param("coreQuestion") String coreQuestion,
                        @Param("aiSummary") String aiSummary,
                        @Param("aiPromptResult") String aiPromptResult,
                        @Param("beliefThen") String beliefThen,
                        @Param("lifeNodeType") LifeNodeType lifeNodeType,
                        @Param("lifeNodeCustomLabel") String lifeNodeCustomLabel,
                        @Param("unlockAt") LocalDateTime unlockAt,
                        @Param("updatedAt") LocalDateTime updatedAt,
                        @Param("draftExpiresAt") LocalDateTime draftExpiresAt);

        int deleteDraftByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

        int saveDraftByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("now") LocalDateTime now);

        int sealSavedByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("sealedAt") LocalDateTime sealedAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

        List<Record> selectExpiredSealedRecords(
                        @Param("now") LocalDateTime now,
                        @Param("limit") int limit);

        List<Record> selectExpiredDrafts(
                        @Param("now") LocalDateTime now,
                        @Param("limit") int limit);

        Record selectExpiredDraftForUpdate(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("expectedExpiresAt") LocalDateTime expectedExpiresAt,
                        @Param("now") LocalDateTime now);

        int deleteExpiredDraftByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("expectedExpiresAt") LocalDateTime expectedExpiresAt,
                        @Param("now") LocalDateTime now);

        int unlockSealedById(
                        @Param("id") Long id,
                        @Param("unlockedAt") LocalDateTime unlockedAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

        int updateLaterReflectionByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("realityLater") String realityLater,
                        @Param("updatedAt") LocalDateTime updatedAt);

        int updateCoverAttachmentByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("coverAttachmentId") Long coverAttachmentId,
                        @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * C2：仅覆盖 content 的窄更新，供 Agent 工具追加正文使用。
         * content 由 service 层拼装（既有正文 + 追加段），SQL 侧仍限定 DRAFT，
         * 使封存不可变约束在数据库层同样成立。
         */
        int updateDraftContentByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("content") String content,
                        @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * C2：仅覆盖 unlock_at 的窄更新，供 Agent 工具建议解锁时间使用。
         * 只写解锁时间，**不触发封存**——status 保持 DRAFT。
         */
        int updateDraftUnlockAtByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("unlockAt") LocalDateTime unlockAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

        int clearCoverAttachmentIfMatches(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("coverAttachmentId") Long coverAttachmentId,
                        @Param("updatedAt") LocalDateTime updatedAt);

        int touchDraftByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("now") LocalDateTime now,
                        @Param("draftExpiresAt") LocalDateTime draftExpiresAt);

        long countByUserAndCondition(
                        @Param("userId") Long userId,
                        @Param("status") RecordStatus status,
                        @Param("recordType") RecordType recordType,
                        @Param("tagId") Long tagId,
                        @Param("keyword") String keyword);

        List<Record> selectPageByUserAndCondition(
                        @Param("userId") Long userId,
                        @Param("status") RecordStatus status,
                        @Param("recordType") RecordType recordType,
                        @Param("tagId") Long tagId,
                        @Param("keyword") String keyword,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        long countUnlockedByUser(@Param("userId") Long userId);

        List<Record> selectUnlockedPageByUser(
                        @Param("userId") Long userId,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        long countTimelineByUserAndCondition(
                        @Param("userId") Long userId,
                        @Param("tagId") Long tagId,
                        @Param("createdFrom") LocalDateTime createdFrom,
                        @Param("createdBefore") LocalDateTime createdBefore);

        List<Record> selectTimelinePageByUserAndCondition(
                        @Param("userId") Long userId,
                        @Param("tagId") Long tagId,
                        @Param("createdFrom") LocalDateTime createdFrom,
                        @Param("createdBefore") LocalDateTime createdBefore,
                        @Param("offset") int offset,
                        @Param("pageSize") int pageSize);

        /**
         * C3 agent-memory-retrieval：检索与当前对话相关的历史记录。
         *
         * 契约要点（agent-runtime / backend-core delta）：
         * - userId 谓词无条件存在，无任何跨用户分支；
         * - **不匹配 content**——正文是最高敏字段且无索引，全表扫描代价与隐私面都最高
         * （design 决策 5、决策 11）；
         * - 排除已封存尚未解锁的记录：用户自己都还没到能看的时刻，
         * Agent 提前复述等于替时间拆封（design 决策 7）；
         * - excludeRecordId 排除当前会话正在写的那条，避免把此刻的内容当旧事。
         */
        List<Record> selectMemoryCandidates(
                        @Param("userId") Long userId,
                        @Param("keywords") List<String> keywords,
                        @Param("tagIds") List<Long> tagIds,
                        @Param("excludeRecordId") Long excludeRecordId,
                        @Param("createdFrom") LocalDateTime createdFrom,
                        @Param("limit") int limit);
}
