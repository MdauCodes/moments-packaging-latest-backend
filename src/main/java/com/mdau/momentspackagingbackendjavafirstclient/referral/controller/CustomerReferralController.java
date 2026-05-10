package com.mdau.momentspackagingbackendjavafirstclient.referral.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.referral.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/referral")
@RequiredArgsConstructor
public class CustomerReferralController {

    private final ReferralService referralService;

    @GetMapping("/code")
    public ResponseEntity<ReferralCodeDto> getMyReferralCode(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(referralService.getMyReferralCode(user));
    }

    @GetMapping("/wallet")
    public ResponseEntity<CreditWalletDto> getMyWallet(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(referralService.getMyWallet(user));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<CreditTransactionDto>> getMyTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(
                referralService.getMyTransactions(user,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/referrals")
    public ResponseEntity<List<ReferralEventDto>> getMyReferrals(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(referralService.getMyReferrals(user));
    }
}