package com.flashback.service;

import com.flashback.domain.Record;
import com.flashback.mapper.RecordMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DraftCleanupService {

    private static final int BATCH_SIZE = 50;

    private final RecordMapper recordMapper;
    private final DraftCleanupWorker worker;
    private final Clock clock;

    public DraftCleanupService(RecordMapper recordMapper, DraftCleanupWorker worker, Clock clock) {
        this.recordMapper = recordMapper;
        this.worker = worker;
        this.clock = clock;
    }

    public DraftCleanupReport runBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Record> candidates = recordMapper.selectExpiredDrafts(now, BATCH_SIZE);
        int deleted = 0;
        int retry = 0;
        int skipped = 0;
        for (Record candidate : candidates == null ? List.<Record>of() : candidates) {
            DraftCleanupResult result;
            try {
                result = worker.cleanup(
                        candidate.getId(),
                        candidate.getUserId(),
                        candidate.getDraftExpiresAt(),
                        now);
            } catch (RuntimeException ex) {
                result = DraftCleanupResult.RETRY;
            }
            switch (result) {
                case DELETED -> deleted++;
                case RETRY -> retry++;
                case SKIPPED -> skipped++;
            }
        }
        return new DraftCleanupReport(candidates == null ? 0 : candidates.size(), deleted, retry, skipped);
    }
}
