package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
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
public class AbandonedCartJob {

    private final CartRepository cartRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "AbandonedCartJob",
            lockAtLeastFor = "PT2M", lockAtMostFor = "PT30M")
    @Transactional
    public void run() {
        log.info("AbandonedCartJob started");
        try {
            Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
            int count = cartRepository.markAbandonedBefore(cutoff);
            log.info("AbandonedCartJob: marked {} carts as ABANDONED", count);
        } catch (Exception e) {
            log.error("AbandonedCartJob failed: {}", e.getMessage(), e);
        }
    }
}