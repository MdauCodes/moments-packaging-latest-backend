package com.mdau.momentspackagingbackendjavafirstclient.cart.entity;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart_id",    columnList = "cart_id"),
        @Index(name = "idx_cart_items_product_id", columnList = "product_id")
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

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_snapshot", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPriceSnapshot;

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
}