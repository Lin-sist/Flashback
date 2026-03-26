package com.flashback.service;

import com.flashback.common.page.PageResult;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;

/**
 * 记录模块业务服务。
 */
public interface RecordService {

    RecordDetailVO create(Long userId, CreateRecordRequest request);

    RecordDetailVO update(Long userId, Long id, UpdateRecordRequest request);

    void delete(Long userId, Long id);

    RecordDetailVO seal(Long userId, Long id);

    PageResult<RecordListItemVO> pageMine(Long userId, RecordPageQuery query);

    PageResult<RecordListItemVO> pageMyUnlocked(Long userId, RecordPageQuery query);

    int runUnlockJob();

    RecordDetailVO detail(Long userId, Long id);
}
