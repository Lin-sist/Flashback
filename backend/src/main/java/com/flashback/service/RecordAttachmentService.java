package com.flashback.service;

import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.vo.AttachmentUploadTokenVO;

public interface RecordAttachmentService {

    AttachmentUploadTokenVO createUploadToken(
            Long userId,
            Long recordId,
            CreateAttachmentUploadTokenRequest request);
}
