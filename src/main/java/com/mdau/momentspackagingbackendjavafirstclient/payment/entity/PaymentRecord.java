package com.mdau.momentspackagingbackendjavafirstclient.payment.entity;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_records", indexes = {
        @Index(name = "idx_payment_records_order_id",            columnList = "order_id"),
        @Index(name = "idx_payment_records_checkout_request_id", columnList = "checkout_request_id"),
        @Index(name = "idx_payment_records_status",              columnList = "status"),
        @Index(name = "idx_payment_records_external_reference",  columnList = "external_reference")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentRecordStatus status = PaymentRecordStatus.INITIATED;

    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    @Column(name = "merchant_request_id", length = 100)
    private String merchantRequestId;

    @Column(name = "external_reference", length = 64)
    private String externalReference;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}