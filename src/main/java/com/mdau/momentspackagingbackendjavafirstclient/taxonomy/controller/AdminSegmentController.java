package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto.SegmentDto;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.service.SegmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/segments")
@RequiredArgsConstructor
public class AdminSegmentController {

    private final SegmentService segmentService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<List<SegmentDto>> getAll() {
        return ResponseEntity.ok(segmentService.getAllSegments());
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<SegmentDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(segmentService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<SegmentDto> create(
            @Valid @RequestBody SegmentCreateRequest request) {
        SegmentDto created = segmentService.createSegment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<SegmentDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody SegmentCreateRequest request) {
        return ResponseEntity.ok(segmentService.updateSegment(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID reassignTo,
            @RequestParam(required = false, defaultValue = "false") boolean cascade) {
        segmentService.deleteSegment(id, reassignTo, cascade);
        return ResponseEntity.noContent().build();
    }
}
