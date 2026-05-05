package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.EmailVerificationTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.PasswordResetTokenRepository;
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

    private final RefreshTokenRepository           refreshTokenRepository;
    private final EmailVerificationTokenRepository evTokenRepository;
    private final PasswordResetTokenRepository     prTokenRepository;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @SchedulerLock(name = "RefreshTokenSweepJob",
            lockAtLeastFor = "PT2M", lockAtMostFor = "PT30M")
    @Transactional
    public void sweep() {
        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        evTokenRepository.deleteExpiredAndUsed(Instant.now());
        prTokenRepository.deleteExpiredAndUsed(Instant.now());
        log.info("Token sweep completed");
    }
}