package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResetMonthlyCountersJob {

    private final ProductRepository productRepository;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Africa/Nairobi")
    @SchedulerLock(
        name = "ResetMonthlyCountersJob",
        lockAtLeastFor = "PT5M",
        lockAtMostFor  = "PT30M"
    )
    @Transactional
    public void run() {
        log.info("ResetMonthlyCountersJob started");
        try {
            productRepository.resetAllMonthlyClicks();
            log.info("ResetMonthlyCountersJob completed — monthly clicks reset for all products");
        } catch (Exception e) {
            log.error("ResetMonthlyCountersJob failed: {}", e.getMessage(), e);
        }
    }
}