package com.mdau.momentspackagingbackendjavafirstclient.customer.entity;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlists", indexes = {
        @Index(name = "idx_wishlists_customer_id", columnList = "customer_id"),
        // Added 2026-07-23 — the (customer_id, product_id) unique constraint below only helps
        // lookups that start with customer_id; "who wishlisted this product" needs its own index.
        @Index(name = "idx_wishlists_product_id", columnList = "product_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_wishlist_customer_product",
                columnNames = {"customer_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @PrePersist
    void prePersist() {
        this.addedAt = Instant.now();
    }
}