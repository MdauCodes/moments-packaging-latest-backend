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

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN') or hasAuthority('PERM_AUDIT_VIEW')")
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogDto>> search(
            @RequestParam(required = false) String  entityType,
            @RequestParam(required = false) String  action,
            @RequestParam(required = false) UUID    actorId,
            @RequestParam(required = false) String  entityId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        int cappedSize = Math.min(size, 200);
        var pageable = PageRequest.of(page, cappedSize,
                Sort.by("created_at").descending());

        // Convert to String for native query — null stays null
        String actorIdStr = actorId != null ? actorId.toString() : null;
        String fromStr    = from    != null ? from.toString()    : null;
        String toStr      = to      != null ? to.toString()      : null;

        return ResponseEntity.ok(new PageResponse<>(
                auditLogRepository.search(
                        entityType, action, actorIdStr, entityId,
                        fromStr, toStr, pageable)
                        .map(AuditLogDto::new)));
    }
}