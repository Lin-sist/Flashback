package com.flashback.service;

import com.flashback.common.page.PageResult;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateLaterReflectionRequest;
import com.flashback.dto.UpdateRecordLocationRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.dto.UpdateUnlockReminderAuthorizationRequest;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.TimelineGroupVO;

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

    RecordDetailVO seal(Long userId, Long id);

    RecordDetailVO updateLaterReflection(Long userId, Long id, UpdateLaterReflectionRequest request);

    RecordDetailVO updateUnlockReminderAuthorization(
            Long userId,
            Long id,
            UpdateUnlockReminderAuthorizationRequest request);

    PageResult<RecordListItemVO> pageMine(Long userId, RecordPageQuery query);

    PageResult<RecordListItemVO> pageMyUnlocked(Long userId, RecordPageQuery query);

    List<TimelineGroupVO> timeline(Long userId, RecordTimelineQuery query);

    int runUnlockJob();

    RecordDetailVO detail(Long userId, Long id);
}
