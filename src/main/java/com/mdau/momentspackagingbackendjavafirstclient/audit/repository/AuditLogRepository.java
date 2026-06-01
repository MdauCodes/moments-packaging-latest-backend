package com.mdau.momentspackagingbackendjavafirstclient.audit.repository;

import com.mdau.momentspackagingbackendjavafirstclient.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
        AND   (:action     IS NULL OR a.action     = :action)
        AND   (:actorId    IS NULL OR a.actorId    = :actorId)
        AND   (:entityId   IS NULL OR a.entityId   = :entityId)
        AND   (:from       IS NULL OR a.createdAt >= :from)
        AND   (:to         IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> search(
            @Param("entityType") String entityType,
            @Param("action")     String action,
            @Param("actorId")    UUID   actorId,
            @Param("entityId")   String entityId,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}