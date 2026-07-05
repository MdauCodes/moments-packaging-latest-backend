package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import com.mdau.momentspackagingbackendjavafirstclient.tag.dto.TagDto;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class ProductDto {

    private final UUID   id;
    private final String slug;
    private final String name;
    private final String category;
    private final String description;
    private final Integer moq;
    private final Boolean individualSalesEnabled;
    private final List<String> sizes;
    private final List<String> tags;
    private final List<String> keywords;
    private final String       primaryImageUrl;
    private final List<String> imageUrls;
    private final Boolean      isDiscount;
    private final Integer      discountPercent;
    private final Boolean      isNewArrival;
    private final Boolean      isFastMoving;
    private final String       material;
    private final String       finish;

    /** Current active base price per unit. */
    private final BigDecimal  basePrice;

    /**
     * Previous/original base price — shown struck-through on frontend.
     * Null when no compare-at price is configured.
     */
    private final BigDecimal  originalBasePrice;

    private final PriceUnit   priceUnit;
    private final StockStatus stockStatus;
    private final Integer     leadTimeDays;
    private final Boolean     customizable;
    private final Integer     stockCount;
    private final Integer     lowStockThreshold;

    private final BigDecimal vatRate;
    private final Boolean    vatExempt;

    private final List<ProductPricingTierDto> pricingTiers;
    private final List<IndustryDto> industries;
    private final List<UUID>        industryIds;
    private final UUID              subcategoryId;
    private final String            subcategoryName;
    private final String            categoryName;
    private final String            segmentName;
    private final List<TagDto>      curatedTags;
    private final List<UUID>        curatedTagIds;
    private final Long              monthlyClicks;
    private final Long              totalClicks;
    private final Instant           createdAt;
    private final Instant           updatedAt;

    public ProductDto(Product product, List<ProductPricingTierDto> pricingTiers) {
        this.id                     = product.getId();
        this.slug                   = product.getSlug();
        this.name                   = product.getName();
        this.category               = product.getCategory();
        this.description            = product.getDescription();
        this.moq                    = product.getMoq();
        this.individualSalesEnabled = product.getIndividualSalesEnabled();
        this.sizes                  = product.getSizes();
        this.tags                   = product.getTags();
        this.keywords               = product.getKeywords();
        this.primaryImageUrl        = product.getPrimaryImageUrl();
        this.imageUrls              = product.getImageUrls();
        this.isDiscount             = product.getIsDiscount();
        this.discountPercent        = product.getDiscountPercent();
        this.isNewArrival           = product.getIsNewArrival();
        this.isFastMoving           = product.getIsFastMoving();
        this.material               = product.getMaterial();
        this.finish                 = product.getFinish();
        this.basePrice              = product.getBasePrice();
        this.originalBasePrice      = product.getOriginalBasePrice();
        this.priceUnit              = product.getPriceUnit();
        this.stockStatus            = product.getStockStatus();
        this.leadTimeDays           = product.getLeadTimeDays();
        this.customizable           = product.getCustomizable();
        this.stockCount             = product.getStockCount();
        this.lowStockThreshold      = product.getLowStockThreshold();
        this.vatRate                = product.getVatRate();
        this.vatExempt              = product.getVatExempt();
        this.pricingTiers           = pricingTiers != null
                ? pricingTiers.stream()
                    .sorted(Comparator.comparingInt(t -> (t.getSortOrder() != null ? t.getSortOrder() : 0)))
                    .collect(Collectors.toList())
                : List.of();
        this.monthlyClicks          = product.getMonthlyClicks();
        this.totalClicks            = product.getTotalClicks();
        this.createdAt              = product.getCreatedAt();
        this.updatedAt              = product.getUpdatedAt();
        this.industries             = product.getIndustries().stream()
                                         .map(IndustryDto::new)
                                         .collect(Collectors.toList());
        this.industryIds            = product.getIndustries().stream()
                                         .map(i -> i.getId())
                                         .collect(Collectors.toList());
        this.subcategoryId          = product.getSubcategory() != null ? product.getSubcategory().getId() : null;
        this.subcategoryName        = product.getSubcategory() != null ? product.getSubcategory().getName() : null;
        this.categoryName           = product.getSubcategory() != null ? product.getSubcategory().getCategory().getName() : null;
        this.segmentName            = product.getSubcategory() != null ? product.getSubcategory().getCategory().getSegment().getName() : null;
        this.curatedTags            = product.getCuratedTags().stream()
                                         .map(TagDto::new)
                                         .collect(Collectors.toList());
        this.curatedTagIds          = product.getCuratedTags().stream()
                                         .map(t -> t.getId())
                                         .collect(Collectors.toList());
    }

    public ProductDto(Product product) {
        this(product, List.of());
    }
}