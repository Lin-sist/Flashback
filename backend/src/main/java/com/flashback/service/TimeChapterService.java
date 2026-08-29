package com.flashback.service;

import com.flashback.common.page.PageResult;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.CreateTimeChapterRequest;
import com.flashback.dto.TimeChapterMemberPageQuery;
import com.flashback.dto.TimeChapterPageQuery;
import com.flashback.dto.UpdateTimeChapterRequest;
import com.flashback.vo.TimeChapterDetailVO;
import com.flashback.vo.TimeChapterSummaryVO;

public interface TimeChapterService {

    PageResult<TimeChapterSummaryVO> page(Long userId, TimeChapterPageQuery query);

    TimeChapterDetailVO detail(Long userId, Long chapterId, TimeChapterMemberPageQuery query);

    TimeChapterSummaryVO create(Long userId, CreateTimeChapterRequest request);

    TimeChapterSummaryVO update(Long userId, Long chapterId, UpdateTimeChapterRequest request);

    TimeChapterSummaryVO addMembers(Long userId, Long chapterId, ChangeTimeChapterMembersRequest request);

    TimeChapterSummaryVO removeMembers(Long userId, Long chapterId, ChangeTimeChapterMembersRequest request);

    TimeChapterSummaryVO end(Long userId, Long chapterId, Long expectedVersion);

    TimeChapterSummaryVO reopen(Long userId, Long chapterId, Long expectedVersion);

    void delete(Long userId, Long chapterId, Long expectedVersion);
}
