package com.mdau.momentspackagingbackendjavafirstclient.tag.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Admin-managed product tag, driving the storefront's "What do you need?"
 * quick-find chips. Distinct from the legacy {@code Product.tags} free-text
 * list (kept as-is, vestigial) — this is a real entity so admin/staff can
 * create, rename, and delete tags without a code change.
 */
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tags_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

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
}
