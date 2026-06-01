package com.mdau.momentspackagingbackendjavafirstclient.audit.service;

import com.mdau.momentspackagingbackendjavafirstclient.audit.entity.AuditLog;
import com.mdau.momentspackagingbackendjavafirstclient.audit.repository.AuditLogRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Central service for writing audit log entries.
 *
 * Usage from any service/controller:
 *   auditLogService.log(actor, "PRODUCT", productId.toString(), productName,
 *                       "PRICE_CHANGE", "Admin corrected price",
 *                       "{\"basePrice\":{\"from\":100,\"to\":90}}", request);
 *
 * All writes are async and run in their own transaction so a failure
 * never rolls back the business operation that triggered the log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor,
                    String entityType,
                    String entityId,
                    String entityLabel,
                    String action,
                    String reason,
                    String changesJson,
                    HttpServletRequest request) {

        try {
            AuditLog entry = AuditLog.builder()
                    .actorId(actor != null ? actor.getId() : null)
                    .actorEmail(actor != null ? actor.getEmail() : "system")
                    .actorName(actor != null ? actor.getFullName() : "system")
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityLabel(entityLabel)
                    .action(action)
                    .reason(reason)
                    .changes(changesJson)
                    .ipAddress(extractIp(request))
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("AuditLog write failed: entityType={} entityId={} action={} error={}",
                    entityType, entityId, action, e.getMessage());
        }
    }

    /** Convenience overload without HTTP request (jobs, internal calls). */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystem(String entityType,
                          String entityId,
                          String entityLabel,
                          String action,
                          String reason,
                          String changesJson) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorEmail("system")
                    .actorName("system")
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityLabel(entityLabel)
                    .action(action)
                    .reason(reason)
                    .changes(changesJson)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("AuditLog system write failed: {}", e.getMessage());
        }
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}