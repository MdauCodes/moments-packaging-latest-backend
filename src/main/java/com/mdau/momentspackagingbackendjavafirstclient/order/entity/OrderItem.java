package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order_id",   columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "category_snapshot", length = 100)
    private String categorySnapshot;

    @Column(name = "size_snapshot", length = 100)
    private String sizeSnapshot;

    @Column(name = "material_snapshot", length = 100)
    private String materialSnapshot;

    @Column(name = "finish_snapshot", length = 100)
    private String finishSnapshot;

    /** null = individual unit purchase */
    @Column(name = "collection_name_snapshot", length = 100)
    private String collectionNameSnapshot;

    /** units per collection e.g. 12 for a Dozen */
    @Column(name = "collection_quantity_snapshot")
    private Integer collectionQuantitySnapshot;

    /** number of collections (or units for individual purchases) */
    @Column(nullable = false)
    private Integer quantity;

    /** total physical units = quantity * collectionQuantity (or just quantity) */
    @Column(name = "total_units")
    private Integer totalUnits;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal lineTotal;
}