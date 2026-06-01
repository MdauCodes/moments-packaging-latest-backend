package com.mdau.momentspackagingbackendjavafirstclient.lead.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_leads_email",      columnList = "email", unique = true),
        @Index(name = "idx_leads_created_at", columnList = "created_at"),
        @Index(name = "idx_leads_source",     columnList = "source")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 50)
    private String persona;

    @Column(length = 100)
    private String source;

    @Column(length = 100)
    private String trigger;

    @Column(nullable = false)
    @Builder.Default
    private Boolean contacted = false;

    /**
     * True when this lead was captured while mock mode was active.
     * Excluded from real lead analytics by default.
     * Nullable so ALTER TABLE on existing rows with nulls succeeds.
     * Application always writes false (or true in mock mode) — never null.
     */
    @Column(name = "is_mock", columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isMock = false;
}