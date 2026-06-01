package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_pricing_tiers", indexes = {
        @Index(name = "idx_pricing_tiers_product_id", columnList = "product_id"),
        @Index(name = "idx_pricing_tiers_sort_order", columnList = "product_id, sort_order"),
        @Index(name = "idx_pricing_tiers_uom_id",     columnList = "uom_id")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private ProductUom uom;

    @Column(name = "collection_name", length = 100, nullable = false)
    private String collectionName;

    @Column(name = "uom_description", columnDefinition = "TEXT")
    private String uomDescription;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Current active price per piece. */
    @Column(name = "price_per_unit", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    /**
     * Previous/original price per piece shown as struck-through on the frontend.
     * Null means no compare-at price is displayed.
     * Must be higher than pricePerUnit to be meaningful.
     */
    @Column(name = "original_price_per_unit", precision = 12, scale = 2)
    private BigDecimal originalPricePerUnit;

    /** Total price for the whole UOM (pricePerUnit * quantity). */
    @Column(name = "collection_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal collectionPrice;

    /**
     * Compare-at collection price (originalPricePerUnit * quantity).
     * Null when no compare-at price is set.
     */
    @Column(name = "original_collection_price", precision = 12, scale = 2)
    private BigDecimal originalCollectionPrice;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;
}