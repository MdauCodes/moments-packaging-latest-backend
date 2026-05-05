package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenSweepJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @SchedulerLock(
        name = "RefreshTokenSweepJob",
        lockAtLeastFor = "PT2M",
        lockAtMostFor  = "PT15M"
    )
    @Transactional
    public void run() {
        log.info("RefreshTokenSweepJob started");
        try {
            refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
            log.info("RefreshTokenSweepJob completed");
        } catch (Exception e) {
            log.error("RefreshTokenSweepJob failed: {}", e.getMessage(), e);
        }
    }
}