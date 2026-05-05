package com.mdau.momentspackagingbackendjavafirstclient.enquiry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryCreateResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.service.EnquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/public/enquiries")
@RequiredArgsConstructor
public class PublicEnquiryController {

    private final EnquiryService  enquiryService;
    private final RateLimitConfig rateLimitConfig;

    @PostMapping
    public ResponseEntity<EnquiryCreateResponse> createEnquiry(
            @Valid @RequestBody EnquiryCreateRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkEnquiry(httpRequest);
        EnquiryCreateResponse response = enquiryService.createEnquiry(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/admin/enquiries/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}