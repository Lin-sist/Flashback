package com.flashback.controller.api;

import com.flashback.common.page.PageResult;
import com.flashback.common.response.ApiResponse;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.dto.UpdateLaterReflectionRequest;
import com.flashback.dto.UpdateRecordCoverRequest;
import com.flashback.dto.UpdateRecordLocationRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.dto.UpdateUnlockReminderAuthorizationRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.RecordService;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.TimelinePageVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 记录主链路接口。
 */
@Validated
@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public ApiResponse<RecordDetailVO> create(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody CreateRecordRequest request) {
        return ApiResponse.success(recordService.create(authUser.getUserId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RecordDetailVO> update(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateRecordRequest request) {
        return ApiResponse.success(recordService.update(authUser.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser AuthUser authUser, @PathVariable("id") Long id) {
        recordService.delete(authUser.getUserId(), id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/location")
    public ApiResponse<RecordDetailVO> updateLocation(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateRecordLocationRequest request) {
        return ApiResponse.success(recordService.updateLocation(authUser.getUserId(), id, request));
    }

    @DeleteMapping("/{id}/location")
    public ApiResponse<RecordDetailVO> deleteLocation(@CurrentUser AuthUser authUser, @PathVariable("id") Long id) {
        return ApiResponse.success(recordService.deleteLocation(authUser.getUserId(), id));
    }

    @PutMapping("/{id}/cover")
    public ApiResponse<RecordDetailVO> updateCover(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @RequestBody UpdateRecordCoverRequest request) {
        return ApiResponse.success(recordService.updateCover(authUser.getUserId(), id, request));
    }

    @PostMapping("/{id}/seal")
    public ApiResponse<RecordDetailVO> seal(@CurrentUser AuthUser authUser, @PathVariable("id") Long id) {
        return ApiResponse.success(recordService.seal(authUser.getUserId(), id));
    }

    @PostMapping("/{id}/save")
    public ApiResponse<RecordDetailVO> save(@CurrentUser AuthUser authUser, @PathVariable("id") Long id) {
        return ApiResponse.success(recordService.save(authUser.getUserId(), id));
    }

    @PutMapping("/{id}/later-reflection")
    public ApiResponse<RecordDetailVO> updateLaterReflection(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateLaterReflectionRequest request) {
        return ApiResponse.success(recordService.updateLaterReflection(authUser.getUserId(), id, request));
    }

    @PutMapping("/{id}/unlock-reminder-authorization")
    public ApiResponse<RecordDetailVO> updateUnlockReminderAuthorization(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUnlockReminderAuthorizationRequest request) {
        return ApiResponse.success(recordService.updateUnlockReminderAuthorization(authUser.getUserId(), id, request));
    }

    @GetMapping
    public ApiResponse<PageResult<RecordListItemVO>> page(
            @CurrentUser AuthUser authUser,
            @Valid RecordPageQuery query) {
        return ApiResponse.success(recordService.pageMine(authUser.getUserId(), query));
    }

    @GetMapping("/unlocked")
    public ApiResponse<PageResult<RecordListItemVO>> unlocked(
            @CurrentUser AuthUser authUser,
            @Valid RecordPageQuery query) {
        return ApiResponse.success(recordService.pageMyUnlocked(authUser.getUserId(), query));
    }

    @GetMapping("/timeline")
    public ApiResponse<TimelinePageVO> timeline(
            @CurrentUser AuthUser authUser,
            @Valid RecordTimelineQuery query) {
        return ApiResponse.success(recordService.timeline(authUser.getUserId(), query));
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordDetailVO> detail(@CurrentUser AuthUser authUser, @PathVariable("id") Long id) {
        return ApiResponse.success(recordService.detail(authUser.getUserId(), id));
    }
}
