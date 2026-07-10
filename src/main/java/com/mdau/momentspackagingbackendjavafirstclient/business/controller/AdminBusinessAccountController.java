package com.mdau.momentspackagingbackendjavafirstclient.business.controller;

import com.mdau.momentspackagingbackendjavafirstclient.business.dto.BusinessAccountDto;
import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccountStatus;
import com.mdau.momentspackagingbackendjavafirstclient.business.service.BusinessAccountService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/business-accounts")
@RequiredArgsConstructor
public class AdminBusinessAccountController {

    private final BusinessAccountService businessAccountService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<BusinessAccountDto>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(businessAccountService.listAll(
                q, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<BusinessAccountDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(businessAccountService.getById(id));
    }

    @IsAdmin
    @PatchMapping("/{id}/status")
    public ResponseEntity<BusinessAccountDto> setStatus(
            @PathVariable UUID id,
            @RequestParam BusinessAccountStatus status) {
        return ResponseEntity.ok(businessAccountService.setStatus(id, status));
    }
}
