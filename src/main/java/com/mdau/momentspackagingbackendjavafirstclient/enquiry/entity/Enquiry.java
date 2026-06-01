package com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enquiries", indexes = {
        @Index(name = "idx_enquiries_email",      columnList = "email"),
        @Index(name = "idx_enquiries_source",     columnList = "source"),
        @Index(name = "idx_enquiries_status",     columnList = "status"),
        @Index(name = "idx_enquiries_assigned_to",columnList = "assigned_to_id"),
        @Index(name = "idx_enquiries_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry extends BaseEntity {

    @Column(length = 50)
    private String persona;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String contactName;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 100)
    private String source;

    // â”€â”€ CRM fields â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private EnquiryStatus status = EnquiryStatus.NEW;

    /** Staff member assigned to follow up on this enquiry. */
    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "assigned_to_name", length = 255)
    private String assignedToName;

    /** Internal notes added by staff over time (appended, not replaced). */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    /** Scheduled follow-up date/time. */
    @Column(name = "follow_up_at")
    private Instant followUpAt;

    /** When the enquiry was first responded to. */
    @Column(name = "first_contacted_at")
    private Instant firstContactedAt;

    /** Value estimate for this enquiry (KES). */
    @Column(name = "estimated_value", precision = 14, scale = 2)
    private java.math.BigDecimal estimatedValue;

    /** Products the customer is interested in (free text or comma-separated). */
    @Column(name = "product_interest", length = 500)
    private String productInterest;

    /**
     * True when this enquiry was created while mock mode was active.
     * Excluded from CRM pipeline metrics and real analytics by default.
     */
    @Column(name = "is_mock", nullable = false)
    @Builder.Default
    private Boolean isMock = false;
}