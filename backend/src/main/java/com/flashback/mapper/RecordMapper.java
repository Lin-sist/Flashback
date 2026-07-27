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

        int updateDraftByIdAndUserId(
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
                        @Param("updatedAt") LocalDateTime updatedAt);

        int deleteDraftByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

        int sealDraftByIdAndUserId(
                        @Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("sealedAt") LocalDateTime sealedAt,
                        @Param("updatedAt") LocalDateTime updatedAt);

        List<Record> selectExpiredSealedRecords(
                        @Param("now") LocalDateTime now,
                        @Param("limit") int limit);

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
}
