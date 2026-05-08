package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_pricing_tiers", indexes = {
        @Index(name = "idx_pricing_tiers_product_id", columnList = "product_id"),
        @Index(name = "idx_pricing_tiers_sort_order", columnList = "product_id, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Human-readable collection label e.g. "Half Dozen", "Bale of 1000", "Pack of 25" */
    @Column(name = "collection_name", length = 100, nullable = false)
    private String collectionName;

    /** Exact fixed quantity in this collection e.g. 6, 12, 25, 1000 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Price per single unit when buying this collection */
    @Column(name = "price_per_unit", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    /** Total price for the whole collection (pricePerUnit * quantity) — stored for display convenience */
    @Column(name = "collection_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal collectionPrice;

    /** Display order — smallest collection first */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Legacy range fields — kept for backward compat, nullable */
    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;
}