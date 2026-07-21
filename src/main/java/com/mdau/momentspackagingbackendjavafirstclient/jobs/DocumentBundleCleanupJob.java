package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.service.DocumentBundleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Monthly (1st of the month) — deletes the Cloudinary ETR for bundles uploaded more than 2 months ago and marks them EXPIRED. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentBundleCleanupJob {

    private final DocumentBundleService documentBundleService;

    @Scheduled(cron = "0 0 3 1 * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "DocumentBundleCleanupJob",
            lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void run() {
        log.info("DocumentBundleCleanupJob started");
        try {
            int count = documentBundleService.expireOldEtrs();
            log.info("DocumentBundleCleanupJob: expired {} ETR document(s)", count);
        } catch (Exception e) {
            log.error("DocumentBundleCleanupJob failed: {}", e.getMessage(), e);
        }
    }
}
