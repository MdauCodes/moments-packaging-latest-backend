package com.mdau.momentspackagingbackendjavafirstclient.settings.service;

import com.mdau.momentspackagingbackendjavafirstclient.settings.entity.AppSetting;
import com.mdau.momentspackagingbackendjavafirstclient.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the discount settings a Business Account's auto-issued welcome code
 * reads at issuance time (see BusinessAccountService.issueWelcomeCode) —
 * seeded here, rather than left as code-only defaults, so they actually show
 * up in the admin Settings table for editing (that page only lists rows
 * that already exist, it doesn't let an admin add a brand-new key).
 */
@Slf4j
@Component
@Order(44)
@RequiredArgsConstructor
public class DiscountSettingsSeeder implements ApplicationRunner {

    private final AppSettingRepository settingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIfAbsent("discounts.welcomeCodeEnabled", "true",
                "Whether a new Business Account automatically gets an auto-issued welcome code. Off means no welcome code is issued at signup.");
        seedIfAbsent("discounts.welcomeCodePercent", "5",
                "Percent-off applied by a Business Account's auto-issued welcome code.");
        seedIfAbsent("discounts.welcomeCodeMinOrderAmount", "5000",
                "Minimum order subtotal (Ksh) for a welcome code to be redeemable.");
        seedIfAbsent("discounts.welcomeCodeValidDays", "30",
                "Days after a Business Account is opened before its welcome code expires.");
        log.info("Discount settings seeded");
    }

    private void seedIfAbsent(String key, String defaultValue, String description) {
        if (settingRepository.findByKey(key).isEmpty()) {
            settingRepository.save(AppSetting.builder()
                    .key(key)
                    .value(defaultValue)
                    .description(description)
                    .build());
            log.debug("Seeded setting: {} = {}", key, defaultValue);
        }
    }
}
