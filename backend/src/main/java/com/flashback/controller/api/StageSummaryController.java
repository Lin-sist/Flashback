package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.StageSummaryService;
import com.flashback.vo.StageSummaryVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M3 manual stage summary endpoint.
 */
@RestController
@RequestMapping("/api/stage-summaries")
public class StageSummaryController {

    private final StageSummaryService stageSummaryService;

    public StageSummaryController(StageSummaryService stageSummaryService) {
        this.stageSummaryService = stageSummaryService;
    }

    @PostMapping("/generate")
    public ApiResponse<StageSummaryVO> generate(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(stageSummaryService.generate(authUser.getUserId()));
    }
}
