package com.mdau.momentspackagingbackendjavafirstclient.referral.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.referral.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/referral")
@RequiredArgsConstructor
@IsAdmin
public class AdminReferralController {

    private final ReferralService referralService;
    private final SettingsService settingsService;

    @Value("${app.admin.superadmin-password:}")
    private String superadminPassword;

    @GetMapping("/status")
    public ResponseEntity<ReferralFeatureStatusDto> getStatus() {
        return ResponseEntity.ok(referralService.getFeatureStatus());
    }

    /**
     * DEVELOPER-ONLY: unlock or lock the referral feature (Layer 1).
     * Requires a dev-secret header matching SUPERADMIN_PASSWORD env var.
     * Call this after client pays for the feature.
     */
    @PostMapping("/feature/unlock")
    public ResponseEntity<Map<String, String>> unlockFeature(
            @AuthenticationPrincipal User user,
            @RequestParam boolean unlock,
            @RequestHeader(value = "X-Dev-Secret", required = false) String devSecret) {

        if (devSecret == null || !devSecret.equals(superadminPassword)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Developer secret required"));
        }

        SettingUpdateRequest req = new SettingUpdateRequest();
        req.setKey(ReferralService.KEY_FEATURE_UNLOCKED);
        req.setValue(String.valueOf(unlock));
        req.setDescription("Developer gate — referral feature unlock status");
        settingsService.upsertSetting(req);

        log.info("Referral feature {} by admin {}", unlock ? "UNLOCKED" : "LOCKED", user.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Referral feature " + (unlock ? "unlocked" : "locked"),
                "featureUnlocked", String.valueOf(unlock)));
    }

    /**
     * ADMIN: enable or disable the referral program (Layer 2).
     * Only works when feature is already unlocked by developer.
     */
    @PostMapping("/program/toggle")
    public ResponseEntity<Map<String, String>> toggleProgram(@RequestParam boolean enabled) {
        if (!referralService.isFeatureUnlocked()) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Referral feature is not unlocked. Contact your developer."));
        }
        SettingUpdateRequest req = new SettingUpdateRequest();
        req.setKey(ReferralService.KEY_PROGRAM_ENABLED);
        req.setValue(String.valueOf(enabled));
        req.setDescription("Admin toggle — referral program on/off");
        settingsService.upsertSetting(req);
        return ResponseEntity.ok(Map.of(
                "message", "Referral program " + (enabled ? "enabled" : "disabled"),
                "programEnabled", String.valueOf(enabled)));
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<ReferralTierConfigDto>> getAllTiers() {
        return ResponseEntity.ok(referralService.getAllTiers());
    }

    @PostMapping("/tiers")
    public ResponseEntity<ReferralTierConfigDto> createTier(
            @Valid @RequestBody ReferralTierConfigDto request) {
        return ResponseEntity.status(201).body(referralService.createTier(request));
    }

    @PutMapping("/tiers/{id}")
    public ResponseEntity<ReferralTierConfigDto> updateTier(
            @PathVariable UUID id,
            @Valid @RequestBody ReferralTierConfigDto request) {
        return ResponseEntity.ok(referralService.updateTier(id, request));
    }

    @DeleteMapping("/tiers/{id}")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID id) {
        referralService.deleteTier(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/credits/adjust")
    public ResponseEntity<CreditWalletDto> adjustCredits(
            @Valid @RequestBody AdminCreditAdjustRequest request) {
        return ResponseEntity.ok(referralService.adminAdjustCredits(request));
    }

    @GetMapping("/credits/transactions")
    public ResponseEntity<PageResponse<CreditTransactionDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(
                referralService.getAllTransactions(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/events")
    public ResponseEntity<PageResponse<ReferralEventDto>> getAllReferralEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(
                referralService.getAllReferralEvents(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}