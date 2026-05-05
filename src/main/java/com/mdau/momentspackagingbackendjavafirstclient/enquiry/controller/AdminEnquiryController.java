package com.mdau.momentspackagingbackendjavafirstclient.enquiry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryDto;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryStatusUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/enquiries")
@RequiredArgsConstructor
public class AdminEnquiryController {

    private final EnquiryService enquiryService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<EnquiryDto>> getAllEnquiries(
            @RequestParam(required = false) EnquiryStatus status,
            @PageableDefault(size = 20, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(enquiryService.getAllEnquiries(status, pageable));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<EnquiryDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(enquiryService.getById(id));
    }

    @IsStaffOrAdmin
    @PatchMapping("/{id}")
    public ResponseEntity<EnquiryDto> updateStatus(
            @PathVariable UUID id,
            @RequestBody EnquiryStatusUpdateRequest request) {
        return ResponseEntity.ok(enquiryService.updateStatus(id, request));
    }
}