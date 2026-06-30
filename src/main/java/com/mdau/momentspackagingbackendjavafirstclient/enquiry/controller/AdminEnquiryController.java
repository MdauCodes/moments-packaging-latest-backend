package com.mdau.momentspackagingbackendjavafirstclient.enquiry.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryDto;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto.EnquiryUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.service.EnquiryService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/enquiries")
@RequiredArgsConstructor
public class AdminEnquiryController {

    private final EnquiryRepository enquiryRepository;
    private final EnquiryService    enquiryService;
    private final AuditLogService   auditLogService;

    @IsStaffOrAdmin
    @GetMapping
    public ResponseEntity<PageResponse<EnquiryDto>> getAll(
            @RequestParam(required = false) String        source,
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(required = false) UUID          assignedToId,
            @RequestParam(required = false) String        q,
            @RequestParam(defaultValue = "0")  int        page,
            @RequestParam(defaultValue = "20") int        size) {
        return ResponseEntity.ok(new PageResponse<>(
                enquiryRepository.findAllWithFilters(
                        source, status, assignedToId, q,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                        .map(EnquiryDto::new)));
    }

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<EnquiryDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(enquiryService.getById(id));
    }

    /** CRM update: status, assignee, notes, follow-up date, estimated value. */
    @IsStaffOrAdmin
    @PatchMapping("/{id}")
    public ResponseEntity<EnquiryDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody EnquiryUpdateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {

        EnquiryDto updated = enquiryService.updateEnquiry(id, request, actor.getEmail());

        auditLogService.log(actor, "ENQUIRY", id.toString(),
                updated.getContactName() + " <" + updated.getEmail() + ">",
                "UPDATE",
                request.getNote(),
                buildChangesJson(request),
                AuditLogService.extractIp(httpRequest));

        return ResponseEntity.ok(updated);
    }

    /** Pipeline summary: count per status — used by CRM kanban header. */
    @IsStaffOrAdmin
    @GetMapping("/pipeline/summary")
    public ResponseEntity<Map<String, Long>> pipelineSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (EnquiryStatus s : EnquiryStatus.values()) {
            summary.put(s.name(), 0L);
        }
        enquiryRepository.countByStatus().forEach(row -> {
            EnquiryStatus status = (EnquiryStatus) row[0];
            Long count = (Long) row[1];
            summary.put(status.name(), count);
        });
        return ResponseEntity.ok(summary);
    }

    /** Enquiries due for follow-up right now. */
    @IsStaffOrAdmin
    @GetMapping("/follow-ups/due")
    public ResponseEntity<List<EnquiryDto>> dueFollowUps() {
        return ResponseEntity.ok(
                enquiryRepository.findDueFollowUps(Instant.now())
                        .stream()
                        .map(EnquiryDto::new)
                        .toList());
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        if (!enquiryRepository.existsById(id))
            throw new ResourceNotFoundException("Enquiry not found: " + id);
        enquiryRepository.deleteById(id);
        auditLogService.log(actor, "ENQUIRY", id.toString(), id.toString(),
                "DELETE", "Admin deleted enquiry", null, AuditLogService.extractIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private String buildChangesJson(EnquiryUpdateRequest r) {
        StringBuilder sb = new StringBuilder("{");
        if (r.getStatus()       != null) sb.append("\"status\":\"").append(r.getStatus()).append("\",");
        if (r.getAssignedToId() != null) sb.append("\"assignedToId\":\"").append(r.getAssignedToId()).append("\",");
        if (r.getFollowUpAt()   != null) sb.append("\"followUpAt\":\"").append(r.getFollowUpAt()).append("\",");
        if (r.getEstimatedValue()!= null) sb.append("\"estimatedValue\":").append(r.getEstimatedValue()).append(",");
        String result = sb.toString();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);
        return result + "}";
    }
}