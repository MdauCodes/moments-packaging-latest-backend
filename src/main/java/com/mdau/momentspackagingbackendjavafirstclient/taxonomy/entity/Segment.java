package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Top level of the product taxonomy: Segment -> Category -> Subcategory.
 * A Segment answers "what general purpose is this packaging for?"
 * (e.g. "Food Packaging", "Drink Packaging"). Every product belongs to
 * exactly one Subcategory, which fixes its Category and Segment by
 * relation — this is a strict single-path hierarchy, not a tagging
 * system. Cross-cutting relevance (who buys it) is handled separately
 * by Industry, which stays many-to-many.
 */
@Entity
@Table(name = "segments", indexes = {
        @Index(name = "idx_segments_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
