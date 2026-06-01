package com.mdau.momentspackagingbackendjavafirstclient.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor_id",    columnList = "actor_id"),
        @Index(name = "idx_audit_logs_entity_type", columnList = "entity_type"),
        @Index(name = "idx_audit_logs_entity_id",   columnList = "entity_id"),
        @Index(name = "idx_audit_logs_action",      columnList = "action"),
        @Index(name = "idx_audit_logs_created_at",  columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** UUID of the user who performed the action. Null for system/job actions. */
    @Column(name = "actor_id")
    private UUID actorId;

    /** Email/name snapshot of the actor at time of action. */
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    /** e.g. PRODUCT, ORDER, ENQUIRY, USER, SETTING, AUTH */
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /** UUID of the affected record. */
    @Column(name = "entity_id", length = 36)
    private String entityId;

    /** Human-readable label of the entity (e.g. product name, order ref). */
    @Column(name = "entity_label", length = 255)
    private String entityLabel;

    /** e.g. CREATE, UPDATE, DELETE, LOGIN, LOGOUT, STATUS_CHANGE, PRICE_CHANGE */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** Free-text explanation of why the action was taken. */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** JSON snapshot of what changed: {"field": {"from": x, "to": y}} */
    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    /** IP address of the request. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { this.createdAt = Instant.now(); }
}