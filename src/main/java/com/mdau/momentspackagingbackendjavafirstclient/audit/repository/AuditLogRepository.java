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
     * Native SQL search — avoids Hibernate 6 nullable UUID/Instant
     * JPQL binding issues entirely. COALESCE pattern lets each param
     * act as "ignore this filter when null".
     *
     * countQuery required alongside nativeQuery paged queries in Spring Data.
     */
    @Query(
        value = """
            SELECT * FROM audit_logs a
            WHERE (:entityType IS NULL OR a.entity_type = :entityType)
            AND   (:action     IS NULL OR a.action      = :action)
            AND   (:actorId    IS NULL OR a.actor_id    = CAST(:actorId AS uuid))
            AND   (:entityId   IS NULL OR a.entity_id   = :entityId)
            AND   (:fromTs     IS NULL OR a.created_at >= CAST(:fromTs AS timestamptz))
            AND   (:toTs       IS NULL OR a.created_at <= CAST(:toTs   AS timestamptz))
            ORDER BY a.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM audit_logs a
            WHERE (:entityType IS NULL OR a.entity_type = :entityType)
            AND   (:action     IS NULL OR a.action      = :action)
            AND   (:actorId    IS NULL OR a.actor_id    = CAST(:actorId AS uuid))
            AND   (:entityId   IS NULL OR a.entity_id   = :entityId)
            AND   (:fromTs     IS NULL OR a.created_at >= CAST(:fromTs AS timestamptz))
            AND   (:toTs       IS NULL OR a.created_at <= CAST(:toTs   AS timestamptz))
            """,
        nativeQuery = true
    )
    Page<AuditLog> search(
            @Param("entityType") String  entityType,
            @Param("action")     String  action,
            @Param("actorId")    String  actorId,
            @Param("entityId")   String  entityId,
            @Param("fromTs")     String  fromTs,
            @Param("toTs")       String  toTs,
            Pageable             pageable);
}