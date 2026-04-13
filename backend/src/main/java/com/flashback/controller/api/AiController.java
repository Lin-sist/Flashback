package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.AiService;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.AiWritingPromptsVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 辅助接口。
 */
@Validated
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/writing-prompts")
    public ApiResponse<AiWritingPromptsVO> writingPrompts(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody AiWritingPromptsRequest request) {
        return ApiResponse.success(aiService.generateWritingPrompts(authUser.getUserId(), request));
    }

    @PostMapping("/summarize-record")
    public ApiResponse<AiSummaryVO> summarizeRecord(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody AiSummarizeRecordRequest request) {
        return ApiResponse.success(aiService.summarizeRecord(authUser.getUserId(), request));
    }
}
