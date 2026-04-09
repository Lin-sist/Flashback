package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.CreateReplyRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.ReplyService;
import com.flashback.vo.ReplyVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 回信主链路接口。
 */
@Validated
@RestController
@RequestMapping("/api/records/{recordId}/reply")
public class ReplyController {

    private final ReplyService replyService;

    public ReplyController(ReplyService replyService) {
        this.replyService = replyService;
    }

    @PostMapping
    public ApiResponse<ReplyVO> create(
            @CurrentUser AuthUser authUser,
            @PathVariable("recordId") Long recordId,
            @Valid @RequestBody CreateReplyRequest request) {
        return ApiResponse.success(replyService.create(authUser.getUserId(), recordId, request));
    }

    @GetMapping
    public ApiResponse<ReplyVO> detail(
            @CurrentUser AuthUser authUser,
            @PathVariable("recordId") Long recordId) {
        return ApiResponse.success(replyService.detail(authUser.getUserId(), recordId));
    }
}
