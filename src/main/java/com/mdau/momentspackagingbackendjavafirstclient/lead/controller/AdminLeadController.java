package com.mdau.momentspackagingbackendjavafirstclient.lead.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadDto;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.lead.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/leads")
@RequiredArgsConstructor
public class AdminLeadController {

    private final LeadService leadService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<LeadDto>> getAllLeads(
            @PageableDefault(size = 20, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getAllLeads(pageable));
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}")
    public ResponseEntity<LeadDto> updateLead(
            @PathVariable UUID id,
            @RequestBody LeadUpdateRequest request) {
        return ResponseEntity.ok(leadService.updateLead(id, request));
    }
}