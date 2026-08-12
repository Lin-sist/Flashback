package com.flashback.schedule;

import com.flashback.service.impl.DataOwnershipServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;

@Component
public class DataOwnershipOperationRecoveryJob {
    private final ObjectProvider<DataOwnershipServiceImpl> serviceProvider;
    public DataOwnershipOperationRecoveryJob(ObjectProvider<DataOwnershipServiceImpl> serviceProvider) { this.serviceProvider = serviceProvider; }
    @Scheduled(fixedDelayString = "${app.data-ownership.recovery-delay-ms:60000}")
    public void resumeStaleOperations() {
        DataOwnershipServiceImpl service = serviceProvider.getIfAvailable();
        if (service != null) service.resumeStaleOperations();
    }
}
