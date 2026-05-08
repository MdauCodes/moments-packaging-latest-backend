package com.mdau.momentspackagingbackendjavafirstclient.cart.entity;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart_id",    columnList = "cart_id"),
        @Index(name = "idx_cart_items_product_id", columnList = "product_id"),
        @Index(name = "idx_cart_items_tier_id",    columnList = "tier_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The collection tier chosen. NULL means individual unit purchase
     * (only valid when product.individualSalesEnabled = true).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private ProductPricingTier tier;

    /**
     * Number of collections (not units).
     * e.g. quantity=3 + tier="Dozen(12)" means 36 physical units.
     * For individual purchases, this is the actual unit count.
     */
    @Column(nullable = false)
    private Integer quantity;

    /** Snapshot of tier.collectionName at time of adding. Null for individual purchases. */
    @Column(name = "collection_name_snapshot", length = 100)
    private String collectionNameSnapshot;

    /** Snapshot of tier.quantity (units per collection) at time of adding. Null for individual. */
    @Column(name = "collection_quantity_snapshot")
    private Integer collectionQuantitySnapshot;

    /** Price per unit at time of adding */
    @Column(name = "unit_price_snapshot", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPriceSnapshot;

    /** Total price for this cart line (collectionPrice * quantity for collections, unitPrice * quantity for individual) */
    @Column(name = "line_total_snapshot", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotalSnapshot;

    @Column(name = "product_name_snapshot", length = 255, nullable = false)
    private String productNameSnapshot;

    @Column(name = "size_snapshot", length = 100)
    private String sizeSnapshot;

    @Column(name = "material_snapshot", length = 100)
    private String materialSnapshot;

    @Column(name = "finish_snapshot", length = 100)
    private String finishSnapshot;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @PrePersist
    void prePersist() {
        this.addedAt = Instant.now();
    }

    /** Convenience: total physical units this line represents */
    @Transient
    public int getTotalUnits() {
        if (tier != null && collectionQuantitySnapshot != null) {
            return quantity * collectionQuantitySnapshot;
        }
        return quantity;
    }
}