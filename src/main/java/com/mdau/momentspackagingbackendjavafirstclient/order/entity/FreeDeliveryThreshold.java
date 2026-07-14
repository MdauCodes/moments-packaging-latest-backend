package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Admin-configured "spend KES X, get free delivery to zone Y" thresholds shown
 * as a cart/checkout FAB banner. Multiple rows allow tiered zones (e.g. CBD at
 * a lower threshold, nationwide at a higher one) — the customer-facing banner
 * always targets the lowest unmet threshold.
 */
@Entity
@Table(name = "free_delivery_thresholds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeDeliveryThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "min_order_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal minOrderAmount;

    /** e.g. "Nairobi CBD", "Anywhere in Kenya" — shown directly in the banner. */
    @Column(name = "zone_label", nullable = false, length = 100)
    private String zoneLabel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
