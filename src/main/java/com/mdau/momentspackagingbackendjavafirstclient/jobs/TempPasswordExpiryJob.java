package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Runs every hour.
 * Soft-deletes staff accounts whose temporary password expired before
 * they could log in for the first time.
 * SUPER_ADMIN must re-invite them to restore access.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempPasswordExpiryJob {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 * * * *") // every hour
    @SchedulerLock(name = "TempPasswordExpiryJob", lockAtLeastFor = "PT55M", lockAtMostFor = "PT58M")
    @Transactional
    public void run() {
        List<User> expired = userRepository
                .findByMustChangePasswordTrueAndTempPasswordExpiresAtBeforeAndDeletedFalse(
                        Instant.now());

        if (expired.isEmpty()) return;

        for (User user : expired) {
            refreshTokenRepository.revokeAllByUser(user);
            user.setDeleted(true);
            user.setEnabled(false);
            user.setDeletedAt(Instant.now());
            userRepository.save(user);
            log.info("Auto-deleted expired unactivated staff account: {}", user.getEmail());
        }

        log.info("TempPasswordExpiryJob: deleted {} expired staff accounts", expired.size());
    }
}