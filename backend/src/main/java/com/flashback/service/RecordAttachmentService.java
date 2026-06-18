package com.flashback.service;

import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.vo.AttachmentAccessUrlVO;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.RecordAttachmentVO;

public interface RecordAttachmentService {

    AttachmentUploadTokenVO createUploadToken(
            Long userId,
            Long recordId,
            CreateAttachmentUploadTokenRequest request);

    RecordAttachmentVO commitAttachment(
            Long userId,
            Long recordId,
            CommitRecordAttachmentRequest request);

    AttachmentAccessUrlVO createAccessUrl(Long userId, Long recordId, Long attachmentId);
}
