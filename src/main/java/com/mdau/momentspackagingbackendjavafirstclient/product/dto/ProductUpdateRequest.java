package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductUpdateRequest {

    private String name;

    private String category;

    private String description;

    @Min(value = 1, message = "MOQ must be at least 1")
    private Integer moq;

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

    private List<UUID> industryIds;
}