package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Phase 4 — every Friday, deletes the Cloudinary PDF for tax invoices emailed more than 2 weeks ago and marks them EXPIRED. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaxDocumentCleanupJob {

    private final TaxDocumentService taxDocumentService;

    @Scheduled(cron = "0 0 3 * * FRI", zone = "Africa/Nairobi")
    @SchedulerLock(name = "TaxDocumentCleanupJob",
            lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void run() {
        log.info("TaxDocumentCleanupJob started");
        try {
            int count = taxDocumentService.expireOldSentDocuments();
            log.info("TaxDocumentCleanupJob: expired {} tax document(s)", count);
        } catch (Exception e) {
            log.error("TaxDocumentCleanupJob failed: {}", e.getMessage(), e);
        }
    }
}
