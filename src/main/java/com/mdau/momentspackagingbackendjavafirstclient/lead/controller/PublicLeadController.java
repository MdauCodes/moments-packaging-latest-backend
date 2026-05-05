package com.mdau.momentspackagingbackendjavafirstclient.lead.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.lead.dto.LeadCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.lead.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/leads")
@RequiredArgsConstructor
public class PublicLeadController {

    private final LeadService     leadService;
    private final RateLimitConfig rateLimitConfig;

    @PostMapping
    public ResponseEntity<LeadCreateResponse> createLead(
            @Valid @RequestBody LeadCreateRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkLead(httpRequest);
        LeadCreateResponse response = leadService.createLead(request);
        return ResponseEntity.ok(response);
    }
}