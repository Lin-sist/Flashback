package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.RecordAttachmentService;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.RecordAttachmentVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * M4 record attachment APIs.
 */
@Validated
@RestController
@RequestMapping("/api/records/{recordId}/attachments")
public class RecordAttachmentController {

    private final RecordAttachmentService recordAttachmentService;

    public RecordAttachmentController(RecordAttachmentService recordAttachmentService) {
        this.recordAttachmentService = recordAttachmentService;
    }

    @PostMapping("/upload-token")
    public ApiResponse<AttachmentUploadTokenVO> createUploadToken(
            @CurrentUser AuthUser authUser,
            @PathVariable("recordId") Long recordId,
            @Valid @RequestBody CreateAttachmentUploadTokenRequest request) {
        return ApiResponse.success(recordAttachmentService.createUploadToken(authUser.getUserId(), recordId, request));
    }

    @PostMapping("/commit")
    public ApiResponse<RecordAttachmentVO> commitAttachment(
            @CurrentUser AuthUser authUser,
            @PathVariable("recordId") Long recordId,
            @Valid @RequestBody CommitRecordAttachmentRequest request) {
        return ApiResponse.success(recordAttachmentService.commitAttachment(authUser.getUserId(), recordId, request));
    }
}
