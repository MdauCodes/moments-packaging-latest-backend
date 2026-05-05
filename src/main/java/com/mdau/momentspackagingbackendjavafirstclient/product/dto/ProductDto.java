package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.industry.dto.IndustryDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class ProductDto {

    private final UUID id;
    private final String slug;
    private final String name;
    private final String category;
    private final String description;
    private final Integer moq;
    private final List<String> sizes;
    private final List<String> tags;
    private final List<String> keywords;
    private final String primaryImageUrl;
    private final List<String> imageUrls;
    private final Boolean isDiscount;
    private final Integer discountPercent;
    private final Boolean isNewArrival;
    private final Boolean isFastMoving;
    private final String material;
    private final String finish;
    private final List<IndustryDto> industries;
    private final List<UUID> industryIds;
    private final Long monthlyClicks;
    private final Long totalClicks;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ProductDto(Product product) {
        this.id              = product.getId();
        this.slug            = product.getSlug();
        this.name            = product.getName();
        this.category        = product.getCategory();
        this.description     = product.getDescription();
        this.moq             = product.getMoq();
        this.sizes           = product.getSizes();
        this.tags            = product.getTags();
        this.keywords        = product.getKeywords();
        this.primaryImageUrl = product.getPrimaryImageUrl();
        this.imageUrls       = product.getImageUrls();
        this.isDiscount      = product.getIsDiscount();
        this.discountPercent = product.getDiscountPercent();
        this.isNewArrival    = product.getIsNewArrival();
        this.isFastMoving    = product.getIsFastMoving();
        this.material        = product.getMaterial();
        this.finish          = product.getFinish();
        this.monthlyClicks   = product.getMonthlyClicks();
        this.totalClicks     = product.getTotalClicks();
        this.createdAt       = product.getCreatedAt();
        this.updatedAt       = product.getUpdatedAt();
        this.industries      = product.getIndustries().stream()
                                   .map(IndustryDto::new)
                                   .collect(Collectors.toList());
        this.industryIds     = product.getIndustries().stream()
                                   .map(i -> i.getId())
                                   .collect(Collectors.toList());
    }
}