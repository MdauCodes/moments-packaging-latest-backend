package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductClickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RollUpProductClicksJob {

    private final ProductClickRepository productClickRepository;

    @Scheduled(cron = "0 30 0 * * *", zone = "Africa/Nairobi")
    @SchedulerLock(
        name = "RollUpProductClicksJob",
        lockAtLeastFor = "PT5M",
        lockAtMostFor  = "PT30M"
    )
    @Transactional
    public void run() {
        log.info("RollUpProductClicksJob started");
        try {
            Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
            productClickRepository.rollUpMonthlyClicks(since);
            Instant cutoff = Instant.now().minus(60, ChronoUnit.DAYS);
            productClickRepository.deleteOlderThan(cutoff);
            log.info("RollUpProductClicksJob completed");
        } catch (Exception e) {
            log.error("RollUpProductClicksJob failed: {}", e.getMessage(), e);
        }
    }
}