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
public class RisellerStockSyncJob {

    private final RisellerSyncService syncService;

    // Every 2 hours at :05 past, Nairobi time
    @Scheduled(cron = "0 5 */2 * * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "RisellerStockSyncJob",
            lockAtLeastFor = "PT5M", lockAtMostFor = "PT50M")
    public void run() {
        log.info("RisellerStockSyncJob: triggered");
        try {
            syncService.syncStock();
        } catch (Exception e) {
            log.error("RisellerStockSyncJob failed: {}", e.getMessage(), e);
        }
    }
}