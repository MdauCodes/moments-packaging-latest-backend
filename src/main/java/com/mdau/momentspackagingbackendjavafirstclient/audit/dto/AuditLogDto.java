package com.mdau.momentspackagingbackendjavafirstclient.audit.dto;

import com.mdau.momentspackagingbackendjavafirstclient.audit.entity.AuditLog;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class AuditLogDto {

    private final UUID    id;
    private final UUID    actorId;
    private final String  actorEmail;
    private final String  actorName;
    private final String  entityType;
    private final String  entityId;
    private final String  entityLabel;
    private final String  action;
    private final String  reason;
    private final String  changes;
    private final String  ipAddress;
    private final Instant createdAt;

    public AuditLogDto(AuditLog log) {
        this.id          = log.getId();
        this.actorId     = log.getActorId();
        this.actorEmail  = log.getActorEmail();
        this.actorName   = log.getActorName();
        this.entityType  = log.getEntityType();
        this.entityId    = log.getEntityId();
        this.entityLabel = log.getEntityLabel();
        this.action      = log.getAction();
        this.reason      = log.getReason();
        this.changes     = log.getChanges();
        this.ipAddress   = log.getIpAddress();
        this.createdAt   = log.getCreatedAt();
    }
}