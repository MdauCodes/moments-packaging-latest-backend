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

    /** null = no change. false = collections-only. true = individual units allowed. */
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

    private BigDecimal basePrice;
    private PriceUnit  priceUnit;
    private StockStatus stockStatus;
    private Integer leadTimeDays;
    private Boolean customizable;
    private Integer stockCount;
    private Integer lowStockThreshold;

    // VAT — null means no change
    private BigDecimal vatRate;
    private Boolean   vatExempt;

    private List<UUID>                  industryIds;
    private List<ProductPricingTierDto> pricingTiers;
}