package com.flashback.service;

import com.flashback.common.page.PageResult;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateLaterReflectionRequest;
import com.flashback.dto.UpdateRecordCoverRequest;
import com.flashback.dto.UpdateRecordLocationRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.dto.UpdateUnlockReminderAuthorizationRequest;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.TimelinePageVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记录模块业务服务。
 */
public interface RecordService {

    RecordDetailVO create(Long userId, CreateRecordRequest request);

    RecordDetailVO update(Long userId, Long id, UpdateRecordRequest request);

    void delete(Long userId, Long id);

    RecordDetailVO updateLocation(Long userId, Long id, UpdateRecordLocationRequest request);

    RecordDetailVO deleteLocation(Long userId, Long id);

    RecordDetailVO updateCover(Long userId, Long id, UpdateRecordCoverRequest request);

    RecordDetailVO seal(Long userId, Long id);

    /**
     * C2：把一段素材追加到草稿正文末尾。
     *
     * 与 {@link #update} 的区别（design.md 决策 5）：update 是全量覆盖且要求 content 非空，
     * 用它来「只加一段」会被迫提交整个记录快照，并发时可能用旧快照覆盖新内容。
     * 本方法只动 content，且**只追加不覆写**——既有正文逐字保留。
     */
    RecordDetailVO appendContent(Long userId, Long id, String text);

    /**
     * C2：在草稿既有标签基础上追加标签。
     *
     * 只追加：既有标签全部保留，重复标签不产生重复绑定，不创建新标签。
     */
    RecordDetailVO appendTags(Long userId, Long id, List<Long> tagIds);

    /**
     * C2：为草稿设置解锁时间。
     *
     * **不触发封存**：记录仍为 DRAFT，仍可继续编辑，封存须由用户自行确认。
     */
    RecordDetailVO updateUnlockAt(Long userId, Long id, LocalDateTime unlockAt);

    RecordDetailVO updateLaterReflection(Long userId, Long id, UpdateLaterReflectionRequest request);

    RecordDetailVO updateUnlockReminderAuthorization(
            Long userId,
            Long id,
            UpdateUnlockReminderAuthorizationRequest request);

    PageResult<RecordListItemVO> pageMine(Long userId, RecordPageQuery query);

    PageResult<RecordListItemVO> pageMyUnlocked(Long userId, RecordPageQuery query);

    TimelinePageVO timeline(Long userId, RecordTimelineQuery query);

    int runUnlockJob();

    RecordDetailVO detail(Long userId, Long id);
}
