package com.mdau.momentspackagingbackendjavafirstclient.industry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryDto;
import com.mdau.momentspackagingbackendjavafirstclient.industry.service.IndustryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/industries")
@RequiredArgsConstructor
public class AdminIndustryController {

    private final IndustryService industryService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<IndustryDto>> getAll() {
        return ResponseEntity.ok(industryService.getAllIndustries());
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<IndustryDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(industryService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<IndustryDto> create(
            @Valid @RequestBody IndustryCreateRequest request) {
        IndustryDto created = industryService.createIndustry(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<IndustryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody IndustryCreateRequest request) {
        return ResponseEntity.ok(industryService.updateIndustry(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID reassignTo) {
        industryService.deleteIndustry(id, reassignTo);
        return ResponseEntity.noContent().build();
    }
}