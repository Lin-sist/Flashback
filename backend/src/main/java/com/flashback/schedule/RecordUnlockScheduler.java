package com.flashback.schedule;

import com.flashback.service.RecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 到期记录解锁调度任务。
 */
@Component
public class RecordUnlockScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordUnlockScheduler.class);

    private final RecordService recordService;

    public RecordUnlockScheduler(RecordService recordService) {
        this.recordService = recordService;
    }

    @Scheduled(cron = "${app.record.unlock-job-cron:0 */1 * * * *}")
    public void runUnlockJob() {
        int unlockedCount = recordService.runUnlockJob();
        if (unlockedCount > 0) {
            log.info("unlock job finished, unlockedCount={}", unlockedCount);
        }
    }
}
