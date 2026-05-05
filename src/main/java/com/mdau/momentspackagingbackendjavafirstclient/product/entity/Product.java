package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_slug",           columnList = "slug",           unique = true),
        @Index(name = "idx_products_category",       columnList = "category"),
        @Index(name = "idx_products_is_discount",    columnList = "is_discount"),
        @Index(name = "idx_products_is_new_arrival", columnList = "is_new_arrival"),
        @Index(name = "idx_products_is_fast_moving", columnList = "is_fast_moving"),
        @Index(name = "idx_products_monthly_clicks", columnList = "monthly_clicks")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer moq = 1;

    @ElementCollection
    @CollectionTable(name = "product_sizes",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size", length = 100)
    @Builder.Default
    private List<String> sizes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_tags",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", length = 100)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_keywords",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "keyword", length = 100)
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Column(name = "primary_image_url", length = 512)
    private String primaryImageUrl;

    @ElementCollection
    @CollectionTable(name = "product_image_urls",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 512)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "is_discount", nullable = false)
    @Builder.Default
    private Boolean isDiscount = false;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "is_new_arrival", nullable = false)
    @Builder.Default
    private Boolean isNewArrival = false;

    @Column(name = "is_fast_moving", nullable = false)
    @Builder.Default
    private Boolean isFastMoving = false;

    @Column(length = 100)
    private String material;

    @Column(length = 100)
    private String finish;

    @Column(name = "monthly_clicks", nullable = false)
    @Builder.Default
    private Long monthlyClicks = 0L;

    @Column(name = "total_clicks", nullable = false)
    @Builder.Default
    private Long totalClicks = 0L;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_industries",
        joinColumns        = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "industry_id")
    )
    @Builder.Default
    private Set<Industry> industries = new HashSet<>();
}