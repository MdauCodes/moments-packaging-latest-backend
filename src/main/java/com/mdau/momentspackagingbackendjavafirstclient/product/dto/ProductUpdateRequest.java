package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductUpdateRequest {

    private String name;
    private String category;
    private String description;
    private Integer moq;
    private Boolean individualSalesEnabled;
    private List<String> sizes;
    private List<String> tags;
    private List<String> keywords;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private Boolean isDiscount;
    private Integer discountPercent;
    private Boolean isNewArrival;
    private Boolean isFastMoving;
    private String material;
    private String finish;

    /** Current active price. Null = no change. */
    private BigDecimal basePrice;

    /**
     * Compare-at price (shown struck-through on frontend).
     * Pass null to clear. Pass a value higher than basePrice to show a sale price.
     */
    private BigDecimal originalBasePrice;

    /** Pass Boolean.FALSE to explicitly clear the compare-at price. */
    private Boolean clearOriginalBasePrice;

    private PriceUnit  priceUnit;
    private StockStatus stockStatus;
    private Integer leadTimeDays;
    private Boolean customizable;
    private Integer stockCount;
    private Integer lowStockThreshold;
    private BigDecimal vatRate;
    private Boolean   vatExempt;

    private List<UUID>                  industryIds;
    private List<ProductPricingTierDto> pricingTiers;

    /** Real Segment/Category/Subcategory classification — optional, can also be set via Classify Products. */
    private UUID       subcategoryId;
    private List<UUID> tagIds;
}