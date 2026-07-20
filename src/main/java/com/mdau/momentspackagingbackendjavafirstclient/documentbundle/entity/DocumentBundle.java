package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * The ETR-gated delivery of an order's receipt + tax invoice + ETR scan, created once payment
 * succeeds for any order where the customer checked "Send me my ETR & tax documents"
 * (Order.etrRequested). Nothing is emailed to the customer until an admin uploads the ETR here —
 * see DocumentBundleService for the upload-then-send lifecycle.
 */
@Entity
@Table(name = "document_bundles", indexes = {
        @Index(name = "idx_document_bundles_order_id", columnList = "order_id"),
        @Index(name = "idx_document_bundles_status", columnList = "status"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentBundle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DocumentBundleStatus status = DocumentBundleStatus.PENDING;

    @Column(name = "etr_cloudinary_url", length = 500)
    private String etrCloudinaryUrl;

    @Column(name = "etr_cloudinary_public_id", length = 255)
    private String etrCloudinaryPublicId;

    @Column(name = "etr_uploaded_at")
    private Instant etrUploadedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;
}
