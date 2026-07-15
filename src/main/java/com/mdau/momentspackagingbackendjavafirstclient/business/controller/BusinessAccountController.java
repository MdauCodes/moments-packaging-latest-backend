package com.mdau.momentspackagingbackendjavafirstclient.business.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.service.BusinessAccountService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business-accounts")
@RequiredArgsConstructor
public class BusinessAccountController {

    private final BusinessAccountService businessAccountService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

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
            @Valid @RequestBody BusinessAccountCreateRequest request,
            HttpServletRequest httpRequest) {
        BusinessAccountDto updated = businessAccountService.updateMine(user, request);
        // When impersonating, `user` is the CUSTOMER (that's whose token this is) — the real
        // actor is the admin identified by the impersonatedBy claim, so look them up to audit
        // the action against the admin, not the customer, with onBehalfOfUserId recording whose
        // account was actually changed.
        UUID adminId = AuditLogService.extractOnBehalfOf(httpRequest);
        if (adminId != null) {
            userRepository.findById(adminId).ifPresent(admin ->
                    auditLogService.log(admin, "BUSINESS_ACCOUNT", updated.getId().toString(), updated.getBusinessName(),
                            "UPDATE", "Updated while impersonating this customer's dashboard", null,
                            AuditLogService.extractIp(httpRequest), user.getId()));
        }
        return ResponseEntity.ok(updated);
    }
}
