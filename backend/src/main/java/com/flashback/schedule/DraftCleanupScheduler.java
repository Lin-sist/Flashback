package com.flashback.schedule;

import com.flashback.service.DraftCleanupReport;
import com.flashback.service.DraftCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DraftCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(DraftCleanupScheduler.class);

    private final DraftCleanupService draftCleanupService;

    public DraftCleanupScheduler(DraftCleanupService draftCleanupService) {
        this.draftCleanupService = draftCleanupService;
    }

    @Scheduled(cron = "${app.record.draft-cleanup-cron:0 17 * * * *}")
    public void runCleanupJob() {
        DraftCleanupReport report = draftCleanupService.runBatch();
        if (report.scanned() > 0) {
            log.info(
                    "draft cleanup finished, scanned={}, deleted={}, retry={}, skipped={}",
                    report.scanned(),
                    report.deleted(),
                    report.retry(),
                    report.skipped());
        }
    }
}
