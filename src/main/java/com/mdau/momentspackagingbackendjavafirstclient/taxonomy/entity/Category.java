package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Middle level of the taxonomy: groups products by physical packaging
 * form within a Segment (e.g. "Bags", "Boxes & Trays" under the
 * "Food Packaging" Segment). See Segment.java for the full hierarchy note.
 */
@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_segment", columnList = "segment_id"),
        @Index(name = "idx_categories_segment_slug", columnList = "segment_id, slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "segment_id", nullable = false)
    private Segment segment;

    @Column(nullable = false, length = 100)
    private String name;

    /** Unique within its Segment, not globally — two Segments may each have their own "Bags". */
    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Which industries this category is relevant to — drives the homepage
     * industry tiles: clicking one filters both products AND the category/
     * subcategory browse list to just what's tagged here. Mirrors the
     * product_industries join table pattern on Product.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "category_industries",
        joinColumns        = @JoinColumn(name = "category_id"),
        inverseJoinColumns = @JoinColumn(name = "industry_id")
    )
    @Builder.Default
    private Set<Industry> industries = new HashSet<>();
}
