package com.mdau.momentspackagingbackendjavafirstclient.referral.controller;

import com.mdau.momentspackagingbackendjavafirstclient.referral.dto.ReferralFeatureStatusDto;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/referral")
@RequiredArgsConstructor
public class PublicReferralController {

    private final ReferralService referralService;

    /**
     * Frontend calls this on load to decide what to show:
     * featureUnlocked=false → "Coming Soon"
     * featureUnlocked=true, programEnabled=false → show UI but disabled
     * featureUnlocked=true, programEnabled=true → fully active
     */
    @GetMapping("/status")
    public ResponseEntity<ReferralFeatureStatusDto> getStatus() {
        return ResponseEntity.ok(referralService.getFeatureStatus());
    }
}