package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.product.service.RisellerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RisellerCatalogSyncJob {

    private final RisellerSyncService syncService;

    // Every 2 days at 01:00 Nairobi time
    @Scheduled(cron = "0 0 1 */2 * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "RisellerCatalogSyncJob",
            lockAtLeastFor = "PT10M", lockAtMostFor = "PT55M")
    public void run() {
        log.info("RisellerCatalogSyncJob: triggered");
        try {
            syncService.syncCatalog();
        } catch (Exception e) {
            log.error("RisellerCatalogSyncJob failed: {}", e.getMessage(), e);
        }
    }
}