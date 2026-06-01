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

    /**
     * Flexible search with all-nullable filters.
     *
     * Hibernate 6 + PostgreSQL: nullable UUID params must use the
     * "param IS NULL OR col = param" pattern — Spring Data cannot
     * bind null directly into "col = :param" for UUID columns.
     */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
        AND   (:action     IS NULL OR a.action     = :action)
        AND   (CAST(:actorId AS java.util.UUID) IS NULL
               OR a.actorId = :actorId)
        AND   (:entityId   IS NULL OR a.entityId   = :entityId)
        AND   (CAST(:from AS java.time.Instant) IS NULL
               OR a.createdAt >= :from)
        AND   (CAST(:to AS java.time.Instant) IS NULL
               OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> search(
            @Param("entityType") String  entityType,
            @Param("action")     String  action,
            @Param("actorId")    UUID    actorId,
            @Param("entityId")   String  entityId,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable);
}