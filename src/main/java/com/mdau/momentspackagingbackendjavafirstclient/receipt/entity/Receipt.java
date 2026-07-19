package com.mdau.momentspackagingbackendjavafirstclient.receipt.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

/**
 * A proof-of-payment PDF — distinct from TaxDocument (the opt-in VAT breakdown). Every order
 * gets one automatically the moment payment is confirmed successful, regardless of whether the
 * customer requested a tax invoice. See ReceiptService for the generate/send lifecycle.
 */
@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipts_order_id", columnList = "order_id"),
        @Index(name = "idx_receipts_status", columnList = "status"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.PENDING;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "cloudinary_url", length = 500)
    private String cloudinaryUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "sent_at")
    private java.time.Instant sentAt;
}
