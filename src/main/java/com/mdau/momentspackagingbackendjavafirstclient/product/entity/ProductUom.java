package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_uoms", indexes = {
        @Index(name = "idx_product_uoms_code",   columnList = "code", unique = true),
        @Index(name = "idx_product_uoms_deleted", columnList = "deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUom extends BaseEntity {

    /**
     * Machine-readable code. Defaults: PIECE, PACKET, CARTON, BALE.
     * Custom examples: DOZEN, ROLL, SET
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** Human-readable display name e.g. "Packet", "Carton" */
    @Column(nullable = false, length = 100)
    private String name;

    /** Optional description shown in admin UI */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * true = one of the 4 seeded defaults (PIECE, PACKET, CARTON, BALE).
     * Default UOMs cannot be deleted.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /** Controls display order in admin UOM list and product editor */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}