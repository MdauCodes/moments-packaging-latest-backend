package com.mdau.momentspackagingbackendjavafirstclient.business.controller;

import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.service.BusinessAccountService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business-accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountService businessAccountService;

    @PostMapping
    public ResponseEntity<BusinessAccountDto> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BusinessAccountCreateRequest request) {
        return ResponseEntity.ok(businessAccountService.create(user, request));
    }

    @GetMapping("/me")
    public ResponseEntity<BusinessAccountDto> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(businessAccountService.getMine(user));
    }

    @PutMapping("/me")
    public ResponseEntity<BusinessAccountDto> updateMine(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BusinessAccountCreateRequest request) {
        return ResponseEntity.ok(businessAccountService.updateMine(user, request));
    }
}
