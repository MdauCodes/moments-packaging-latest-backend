package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Lowest level of the taxonomy, one step above an individual product —
 * the specific, customer-recognizable product type (e.g. "Pizza boxes",
 * "T-shirt bags" under the "Boxes & Trays" / "Bags" Categories). Every
 * Product links to exactly one Subcategory (see Product.subcategory).
 * See Segment.java for the full hierarchy note.
 */
@Entity
@Table(name = "subcategories", indexes = {
        @Index(name = "idx_subcategories_category", columnList = "category_id"),
        @Index(name = "idx_subcategories_category_slug", columnList = "category_id, slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    /** Unique within its Category, not globally. */
    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
