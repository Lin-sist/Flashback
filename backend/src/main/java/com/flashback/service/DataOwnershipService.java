package com.flashback.service;

import com.flashback.domain.DataDeletionScope;
import com.flashback.domain.SealedContentPolicy;
import com.flashback.vo.DataOperationVO;
import com.flashback.vo.DataOwnershipSummaryVO;

public interface DataOwnershipService {
    DataOwnershipSummaryVO summary(Long userId);
    DataOperationVO createExport(Long userId, SealedContentPolicy policy);
    DataOperationVO getOperation(Long userId, Long operationId);
    byte[] downloadExport(Long userId, Long operationId);
    DataOperationVO prepareDeletion(Long userId, DataDeletionScope scope, Long recordId);
    DataOperationVO confirmDeletion(Long userId, Long intentId, String confirmationText);
    DataOperationVO retry(Long userId, Long operationId);
}
