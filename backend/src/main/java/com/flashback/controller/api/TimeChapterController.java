package com.flashback.controller.api;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.page.PageResult;
import com.flashback.common.response.ApiResponse;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.CreateTimeChapterRequest;
import com.flashback.dto.TimeChapterMemberPageQuery;
import com.flashback.dto.TimeChapterPageQuery;
import com.flashback.dto.TimeChapterVersionRequest;
import com.flashback.dto.UpdateTimeChapterRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.TimeChapterService;
import com.flashback.vo.TimeChapterDetailVO;
import com.flashback.vo.TimeChapterSummaryVO;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

/**
 * 时间篇章接口。篇章是记录的手动容器，不改变记录本体。
 */
@Validated
@RestController
@RequestMapping("/api/time-chapters")
public class TimeChapterController {

    private final TimeChapterService service;

    public TimeChapterController(TimeChapterService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<TimeChapterSummaryVO>> page(
            @CurrentUser AuthUser authUser,
            @Valid TimeChapterPageQuery query) {
        return ApiResponse.success(service.page(authUser.getUserId(), query));
    }

    @PostMapping
    public ApiResponse<TimeChapterSummaryVO> create(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody CreateTimeChapterRequest request) {
        return ApiResponse.success(mutation(() -> service.create(authUser.getUserId(), request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TimeChapterDetailVO> detail(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid TimeChapterMemberPageQuery query) {
        return ApiResponse.success(service.detail(authUser.getUserId(), id, query));
    }

    @PutMapping("/{id}")
    public ApiResponse<TimeChapterSummaryVO> update(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTimeChapterRequest request) {
        return ApiResponse.success(mutation(() -> service.update(authUser.getUserId(), id, request)));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<TimeChapterSummaryVO> addMembers(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeTimeChapterMembersRequest request) {
        return ApiResponse.success(mutation(() -> service.addMembers(authUser.getUserId(), id, request)));
    }

    @PostMapping("/{id}/members/remove")
    public ApiResponse<TimeChapterSummaryVO> removeMembers(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeTimeChapterMembersRequest request) {
        return ApiResponse.success(mutation(() -> service.removeMembers(authUser.getUserId(), id, request)));
    }

    @PostMapping("/{id}/end")
    public ApiResponse<TimeChapterSummaryVO> end(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody TimeChapterVersionRequest request) {
        return ApiResponse.success(mutation(() -> service.end(authUser.getUserId(), id, request.getExpectedVersion())));
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<TimeChapterSummaryVO> reopen(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody TimeChapterVersionRequest request) {
        return ApiResponse.success(mutation(() -> service.reopen(authUser.getUserId(), id, request.getExpectedVersion())));
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(
            @CurrentUser AuthUser authUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody TimeChapterVersionRequest request) {
        mutation(() -> {
            service.delete(authUser.getUserId(), id, request.getExpectedVersion());
            return null;
        });
        return ApiResponse.success(null);
    }

    private <T> T mutation(Supplier<T> action) {
        try {
            return action.get();
        } catch (DuplicateKeyException | TransientDataAccessException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT,
                    "篇章状态已变更，请刷新后重试");
        }
    }
}
