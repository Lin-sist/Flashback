package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.ConfirmDataDeletionRequest;
import com.flashback.dto.CreateDataExportRequest;
import com.flashback.dto.PrepareDataDeletionRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.DataOwnershipService;
import com.flashback.vo.DataOperationVO;
import com.flashback.vo.DataOwnershipSummaryVO;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data-ownership")
public class DataOwnershipController {
    private final DataOwnershipService service;
    public DataOwnershipController(DataOwnershipService service) { this.service = service; }

    @GetMapping("/summary")
    public ApiResponse<DataOwnershipSummaryVO> summary(@CurrentUser AuthUser user) { return ApiResponse.success(service.summary(user.getUserId())); }

    @PostMapping("/export-operations")
    public ApiResponse<DataOperationVO> export(@CurrentUser AuthUser user, @Valid @RequestBody CreateDataExportRequest request) {
        return ApiResponse.success(service.createExport(user.getUserId(), request.getSealedContentPolicy()));
    }

    @GetMapping("/operations/{id}")
    public ApiResponse<DataOperationVO> operation(@CurrentUser AuthUser user, @PathVariable Long id) { return ApiResponse.success(service.getOperation(user.getUserId(), id)); }

    @GetMapping("/export-operations/{id}/download")
    public ResponseEntity<byte[]> download(@CurrentUser AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("flashback-export.zip").build().toString())
                .body(service.downloadExport(user.getUserId(), id));
    }

    @PostMapping("/deletion-intents")
    public ApiResponse<DataOperationVO> prepareDeletion(@CurrentUser AuthUser user, @Valid @RequestBody PrepareDataDeletionRequest request) {
        return ApiResponse.success(service.prepareDeletion(user.getUserId(), request.getScope(), request.getRecordId()));
    }

    @PostMapping("/deletion-operations")
    public ApiResponse<DataOperationVO> confirmDeletion(@CurrentUser AuthUser user, @Valid @RequestBody ConfirmDataDeletionRequest request) {
        return ApiResponse.success(service.confirmDeletion(user.getUserId(), request.getIntentId(), request.getConfirmationText()));
    }

    @PostMapping("/operations/{id}/retry")
    public ApiResponse<DataOperationVO> retry(@CurrentUser AuthUser user, @PathVariable Long id) { return ApiResponse.success(service.retry(user.getUserId(), id)); }
}
