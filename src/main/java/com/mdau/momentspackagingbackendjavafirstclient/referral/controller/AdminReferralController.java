package com.mdau.momentspackagingbackendjavafirstclient.referral.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.referral.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/referral")
@RequiredArgsConstructor
@IsAdmin
public class AdminReferralController {

    private final ReferralService referralService;

    // ── Tier config CRUD ──────────────────────────────────────────────────────

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

    // ── Credit management ─────────────────────────────────────────────────────

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

    // ── Referral events ───────────────────────────────────────────────────────

    @GetMapping("/events")
    public ResponseEntity<PageResponse<ReferralEventDto>> getAllReferralEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(
                referralService.getAllReferralEvents(
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }
}