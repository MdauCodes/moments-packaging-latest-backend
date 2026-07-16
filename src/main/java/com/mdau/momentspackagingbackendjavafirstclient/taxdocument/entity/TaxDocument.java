package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

/**
 * A tax invoice / VAT breakdown PDF requested by a customer for a specific order. Not an
 * eTIMS-verified receipt (Moments isn't integrated with KRA eTIMS yet) — this is a plain PDF,
 * generated from the order's already-computed taxableAmount/vatAmount, uploaded to Cloudinary,
 * and emailed. See TaxDocumentService for the generate/send lifecycle and the weekly cleanup
 * job that deletes the Cloudinary asset (and flips status to EXPIRED) after 2 weeks.
 */
@Entity
@Table(name = "tax_documents", indexes = {
        @Index(name = "idx_tax_documents_order_id", columnList = "order_id"),
        @Index(name = "idx_tax_documents_status", columnList = "status"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaxDocumentStatus status = TaxDocumentStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cloudinary_url", length = 500)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "sent_at")
    private java.time.Instant sentAt;

    /**
     * One-time secret returned to the frontend at checkout so it (not just anyone who guesses
     * the order reference) can request a Cloudinary upload signature and report the result back.
     * See TaxDocumentController.
     */
    @Column(name = "upload_token", length = 40)
    private String uploadToken;
}
