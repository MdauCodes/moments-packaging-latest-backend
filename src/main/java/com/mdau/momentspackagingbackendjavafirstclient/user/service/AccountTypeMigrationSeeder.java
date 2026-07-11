package com.mdau.momentspackagingbackendjavafirstclient.user.service;

import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration used to create plain, type-less customer accounts. Every
 * customer now must pick Sole Merchant or Business at signup — this backfills
 * pre-existing accounts as Sole Merchant (per Mdau's decision: silent
 * auto-conversion, no forced re-onboarding). Idempotent — only touches rows
 * still null, so it's a no-op after the first run.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class AccountTypeMigrationSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int updated = userRepository.backfillMissingAccountTypeAsSoleMerchant();
        if (updated > 0) {
            log.info("Backfilled accountType=SOLE_MERCHANT for {} existing customer account(s)", updated);
        }
    }
}
