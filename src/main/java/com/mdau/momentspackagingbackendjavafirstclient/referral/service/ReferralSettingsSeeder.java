package com.mdau.momentspackagingbackendjavafirstclient.referral.service;

import com.mdau.momentspackagingbackendjavafirstclient.settings.entity.AppSetting;
import com.mdau.momentspackagingbackendjavafirstclient.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ReferralSettingsSeeder implements ApplicationRunner {

    private final AppSettingRepository settingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIfAbsent("referral.feature.unlocked", "false",
                "Developer gate — set true after client pays for referral feature");
        seedIfAbsent("referral.program.enabled", "false",
                "Admin toggle — enables/disables referral program. Only visible when feature is unlocked");
        seedIfAbsent("referral.credits.per.kes", "10",
                "How many credits equal 1 KES. e.g. 10 means 10 credits = KES 1");
        seedIfAbsent("referral.max.redemption.percent", "20",
                "Maximum % of order total payable with credits e.g. 20 = max 20% discount");
        seedIfAbsent("referral.max.active.referrals.per.user", "50",
                "Max pending referrals one user can have at a time");
        log.info("Referral settings seeded");
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