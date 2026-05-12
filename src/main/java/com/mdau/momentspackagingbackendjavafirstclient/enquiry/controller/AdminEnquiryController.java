package com.mdau.momentspackagingbackendjavafirstclient.enquiry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/enquiries")
@RequiredArgsConstructor
public class AdminEnquiryController {

    private final EnquiryRepository enquiryRepository;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<Enquiry>> getAll(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new PageResponse<>(
                enquiryRepository.findAllWithFilters(
                        source, q,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<Enquiry> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                enquiryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + id)));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!enquiryRepository.existsById(id))
            throw new ResourceNotFoundException("Enquiry not found: " + id);
        enquiryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}