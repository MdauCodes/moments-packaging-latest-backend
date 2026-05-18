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

    /**
     * The UOM this tier represents (e.g. PACKET, CARTON).
     * Nullable for legacy tiers created before UOM system.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private ProductUom uom;

    /** Human-readable collection label e.g. "Packet of 25", "Carton of 50 packets" */
    @Column(name = "collection_name", length = 100, nullable = false)
    private String collectionName;

    /**
     * Describes the contents of this UOM for the customer.
     * e.g. "A packet of 200ml tumblers has 25 pieces"
     */
    @Column(name = "uom_description", columnDefinition = "TEXT")
    private String uomDescription;

    /** Number of pieces in this UOM e.g. 25 for a packet, 1250 for a bale */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Price per single piece when buying this UOM */
    @Column(name = "price_per_unit", precision = 12, scale = 2, nullable = false)
    private BigDecimal pricePerUnit;

    /** Total price for the whole UOM (pricePerUnit * quantity) */
    @Column(name = "collection_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal collectionPrice;

    /** Display order — smallest UOM first */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * When false this UOM is completely hidden from public API responses.
     * Admin can disable UOMs not applicable to a product
     * (e.g. tumblers don't have a BALE — disable it).
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** Legacy range fields — kept for backward compat, nullable */
    @Column(name = "min_quantity")
    private Integer minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;
}