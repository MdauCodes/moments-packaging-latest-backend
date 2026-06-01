package com.mdau.momentspackagingbackendjavafirstclient.settings.service;

import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock / Test Mode — controlled by SUPER_ADMIN only.
 *
 * When mock mode is ON, the system behaves EXACTLY like production:
 *   - Real M-Pesa / PayHero STK pushes are sent.
 *   - Real emails and SMS are delivered.
 *   - All order, payment, checkout, and notification flows run normally.
 *   - Staff experience the full live customer journey end-to-end.
 *
 * The ONLY difference is a flag on the data:
 *   - Orders, enquiries, and leads created during mock mode carry is_mock = true.
 *   - Revenue reports, analytics, and dashboards exclude is_mock = true records
 *     by default so test transactions don't pollute real metrics.
 *   - SUPER_ADMIN can pass includeMock=true to any analytics endpoint to
 *     review test data for QA purposes.
 *
 * Backed by AppSetting key "mock.mode.enabled" (value "true"/"false").
 * Read from DB on every request — no restart needed to toggle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockModeService {

    static final String SETTING_KEY = "mock.mode.enabled";

    private final SettingsService settingsService;

    public boolean isMockModeEnabled() {
        return Boolean.parseBoolean(
                settingsService.getValue(SETTING_KEY, "false"));
    }

    public void setMockMode(boolean enabled, String actorEmail) {
        SettingUpdateRequest req = new SettingUpdateRequest();
        req.setKey(SETTING_KEY);
        req.setValue(String.valueOf(enabled));
        req.setDescription(
                "Test / mock mode. When true all new orders, enquiries, and leads " +
                "are flagged is_mock=true and excluded from real analytics. " +
                "All system flows (payments, emails, SMS) run normally.");
        settingsService.upsertSetting(req);
        log.info("Mock mode {} by {}", enabled ? "ENABLED" : "DISABLED", actorEmail);
    }
}