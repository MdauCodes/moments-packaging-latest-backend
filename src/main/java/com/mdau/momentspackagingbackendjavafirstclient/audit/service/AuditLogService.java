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

/**
 * Central service for writing audit log entries.
 *
 * IMPORTANT: always call extractIp(httpRequest) synchronously in the controller and pass the
 * String result — never pass HttpServletRequest directly. Tomcat recycles the request object
 * after the response is committed, before this @Async method runs on a different thread.
 *
 * Usage:
 *   auditLogService.log(actor, "PRODUCT", id, name, "PRICE_CHANGE", reason, changesJson,
 *                       AuditLogService.extractIp(httpRequest));
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
                    String ipAddress) {

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
                    .ipAddress(ipAddress)
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

    /** Extract the caller IP synchronously on the request thread before any async hand-off. */
    public static String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
