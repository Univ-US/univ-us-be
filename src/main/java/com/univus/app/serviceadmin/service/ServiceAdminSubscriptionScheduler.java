package com.univus.app.serviceadmin.service;

import com.univus.app.serviceadmin.mapper.ServiceAdminSchoolMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceAdminSubscriptionScheduler {

    private final ServiceAdminSchoolMapper serviceAdminSchoolMapper;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void finalizeScheduledCancellations() {
        int updated = serviceAdminSchoolMapper.finalizeScheduledCancellations();
        if (updated > 0) {
            log.info("Finalized {} scheduled subscription cancellations.", updated);
        }
    }
}
