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
 * Seeds the setting key that lets an admin enter the business's own KRA PIN
 * from the Settings page (no code change/redeploy needed to set it). Printed
 * on customer invoices — see PublicConfigController and pdf.ts's masthead.
 */
@Slf4j
@Component
@Order(43)
@RequiredArgsConstructor
public class BusinessTaxSettingsSeeder implements ApplicationRunner {

    private final AppSettingRepository settingRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIfAbsent("business.kraPin", "",
                "KRA PIN printed on customer invoices — set this before customers rely on invoices for their own tax records.");
        log.info("Business tax settings seeded");
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
