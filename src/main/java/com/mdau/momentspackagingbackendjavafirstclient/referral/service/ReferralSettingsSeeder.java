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

        // Sole Merchant rewards program — same wallet/ledger, new earning triggers.
        seedIfAbsent("rewards.welcome.points", "100",
                "Points awarded when a Sole Merchant Account is opened");
        seedIfAbsent("rewards.review.points", "50",
                "Points awarded for submitting a product review");
        seedIfAbsent("rewards.points.per.100kes", "1",
                "Points earned per KES 100 spent on a paid order (Sole Merchant accounts only)");

        launchRewardsProgramOnce();
        log.info("Referral settings seeded");
    }

    /**
     * The referral system was built but deliberately left locked while
     * unfinished. It's now fully wired into checkout/orders as the Sole
     * Merchant rewards ledger, so this flips the two gates to enabled —
     * exactly once, tracked by a sentinel key, so it never fights an admin
     * who later turns the program off again via the Settings UI.
     */
    private void launchRewardsProgramOnce() {
        String sentinelKey = "rewards.launch.migration.done";
        if (settingRepository.findByKey(sentinelKey).isPresent()) return;

        forceSet("referral.feature.unlocked", "true");
        forceSet("referral.program.enabled", "true");
        settingRepository.save(AppSetting.builder()
                .key(sentinelKey)
                .value("true")
                .description("Internal — marks that the Sole Merchant rewards launch flip has run. Do not edit.")
                .build());
        log.info("Rewards program launched: referral.feature.unlocked and referral.program.enabled set to true");
    }

    private void forceSet(String key, String value) {
        settingRepository.findByKey(key).ifPresentOrElse(
                setting -> {
                    setting.setValue(value);
                    settingRepository.save(setting);
                },
                () -> settingRepository.save(AppSetting.builder().key(key).value(value).build()));
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