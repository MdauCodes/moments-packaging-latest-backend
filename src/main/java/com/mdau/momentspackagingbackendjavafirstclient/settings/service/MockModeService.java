package com.mdau.momentspackagingbackendjavafirstclient.settings.service;

import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock Mode — controlled by SUPER_ADMIN only.
 *
 * When mock mode is ON:
 *   - All orders, payments, enquiries, leads created carry is_mock = true.
 *   - Emails and SMS are suppressed (no real messages sent).
 *   - M-Pesa / PayHero STK pushes are skipped; payment is auto-confirmed.
 *   - Mock data is fully stored in the database so all flows can be tested.
 *   - Admin dashboards, audit logs, and analytics exclude mock records by default
 *     (pass includeMock=true to include them for QA review).
 *
 * Backed by AppSetting key "mock.mode.enabled" (value "true"/"false").
 * The flag is read from the DB on every request — no restart needed.
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
        req.setDescription("Mock / test mode — excludes data from real analytics when true");
        settingsService.upsertSetting(req);
        log.info("Mock mode {} by {}", enabled ? "ENABLED" : "DISABLED", actorEmail);
    }
}