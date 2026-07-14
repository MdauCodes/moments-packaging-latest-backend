package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import com.mdau.momentspackagingbackendjavafirstclient.tag.entity.Tag;
import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
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
        @Index(name = "idx_products_monthly_clicks", columnList = "monthly_clicks"),
        @Index(name = "idx_products_stock_status",   columnList = "stock_status"),
        @Index(name = "idx_products_deleted",        columnList = "deleted")
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

    /**
     * New hierarchical classification (Segment -> Category -> Subcategory).
     * Nullable during migration from the legacy {@link #category} string;
     * every product should eventually have exactly one.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer moq = 1;

    @Column(name = "individual_sales_enabled", nullable = false)
    @Builder.Default
    private Boolean individualSalesEnabled = true;

    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size", length = 100)
    @Builder.Default
    private List<String> sizes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", length = 100)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_keywords", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "keyword", length = 100)
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @Column(name = "primary_image_url", length = 512)
    private String primaryImageUrl;

    @ElementCollection
    @CollectionTable(name = "product_image_urls", joinColumns = @JoinColumn(name = "product_id"))
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

    /** Current active base price per unit. */
    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    /**
     * Previous/original base price shown struck-through on the frontend.
     * Null = no compare-at price displayed.
     * Must be higher than basePrice to be displayed.
     */
    @Column(name = "original_base_price", precision = 12, scale = 2)
    private BigDecimal originalBasePrice;

    /**
     * Unit cost (excl. VAT), synced from Riseller's CostInc/UnitCostEx.
     * Null when Riseller hasn't reported a cost for this item yet.
     * Drives margin-aware reward/referral tier calculations — never shown to customers.
     */
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    /** Riseller's own gross-profit percent for this item, synced alongside costPrice. */
    @Column(name = "gross_profit_percent", precision = 6, scale = 2)
    private BigDecimal grossProfitPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", length = 20)
    @Builder.Default
    private PriceUnit priceUnit = PriceUnit.PER_UNIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 20)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    @Column(name = "lead_time_days")
    @Builder.Default
    private Integer leadTimeDays = 14;

    @Column(name = "customizable")
    @Builder.Default
    private Boolean customizable = false;


    @Column(name = "riseller_item_id", unique = true, length = 100)
    private String risellerItemId;

    @Column(name = "stock_count")
    @Builder.Default
    private Integer stockCount = 0;

    @Column(name = "reserved_count")
    @Builder.Default
    private Integer reservedCount = 0;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "vat_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("0.1600");

    @Column(name = "vat_exempt", nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean vatExempt = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * True when this product was previously linked to Riseller but the Riseller item
     * was removed from the catalog. The product is kept (never deleted) so admin-added
     * content (images, descriptions, prices) is preserved. It is hidden from the
     * storefront until Riseller re-lists the item, at which point the catalog sync
     * clears this flag and re-links automatically.
     */
    @Column(name = "riseller_suspended", nullable = false,
            columnDefinition = "boolean not null default false")
    @Builder.Default
    private Boolean risellerSuspended = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_industries",
        joinColumns        = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "industry_id")
    )
    @Builder.Default
    private Set<Industry> industries = new HashSet<>();

    /**
     * Admin-managed curated tags (drives the storefront's "What do you need?"
     * chips). Distinct from the legacy {@link #tags} free-text list above —
     * that field stays untouched/vestigial, this is the real relation.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_tag_assignments",
        joinColumns        = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> curatedTags = new HashSet<>();
}