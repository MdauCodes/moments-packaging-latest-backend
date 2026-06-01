package com.mdau.momentspackagingbackendjavafirstclient.audit.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.dto.AuditLogDto;
import com.mdau.momentspackagingbackendjavafirstclient.audit.repository.AuditLogRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogRepository auditLogRepository;

    /**
     * GET /api/v1/admin/audit-logs
     * Query params: entityType, action, actorId, entityId, from, to, page, size
     *
     * SUPER_ADMIN sees everything.
     * Staff with AUDIT_VIEW permission can see logs.
     */
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasAuthority('PERM_AUDIT_VIEW')")
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDto>> search(
            @RequestParam(required = false) String  entityType,
            @RequestParam(required = false) String  action,
            @RequestParam(required = false) UUID    actorId,
            @RequestParam(required = false) String  entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        int cappedSize = Math.min(size, 200);
        var pageable = PageRequest.of(page, cappedSize, Sort.by("createdAt").descending());

        return ResponseEntity.ok(new PageResponse<>(
                auditLogRepository.search(entityType, action, actorId, entityId, from, to, pageable)
                        .map(AuditLogDto::new)));
    }
}