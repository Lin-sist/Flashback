package com.flashback.schedule;

import com.flashback.domain.DataOperation;
import com.flashback.domain.DataOperationFailureCode;
import com.flashback.domain.DataOperationStatus;
import com.flashback.mapper.DataOperationMapper;
import com.flashback.service.data.DataOwnershipArtifactStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class DataOwnershipArtifactCleanupJob {
    private final DataOperationMapper mapper;
    private final DataOwnershipArtifactStore store;
    private final Clock clock;
    public DataOwnershipArtifactCleanupJob(DataOperationMapper mapper, DataOwnershipArtifactStore store, Clock clock) {
        this.mapper = mapper; this.store = store; this.clock = clock;
    }
    @Scheduled(fixedDelayString = "${app.data-ownership.cleanup-delay-ms:3600000}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(clock);
        for (DataOperation op : mapper.selectExpiredArtifacts(now, 100)) {
            store.delete(op.getArtifactToken());
            op.setStatus(DataOperationStatus.EXPIRED); op.setFailureCode(DataOperationFailureCode.ARTIFACT_EXPIRED); op.setUpdatedAt(now);
            mapper.updateIfStatus(op, DataOperationStatus.SUCCEEDED);
        }
    }
}
