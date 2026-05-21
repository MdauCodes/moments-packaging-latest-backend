package com.mdau.momentspackagingbackendjavafirstclient.product.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
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

    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

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

    @Column(name = "stock_count")
    @Builder.Default
    private Integer stockCount = 0;

    @Column(name = "reserved_count")
    @Builder.Default
    private Integer reservedCount = 0;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    // ── VAT fields ────────────────────────────────────────────────────────────

    /**
     * VAT rate for this product as a decimal e.g. 0.16 = 16%.
     * Default is 16% (Kenya standard VAT rate).
     * Overridden per product by admin.
     */
    @Column(name = "vat_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("0.1600");

    /**
     * When true, this product is VAT-exempt — no VAT is charged.
     * vatRate is ignored when this is true.
     */
    @Column(name = "vat_exempt", nullable = false)
    @Builder.Default
    private Boolean vatExempt = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_industries",
        joinColumns        = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "industry_id")
    )
    @Builder.Default
    private Set<Industry> industries = new HashSet<>();
}