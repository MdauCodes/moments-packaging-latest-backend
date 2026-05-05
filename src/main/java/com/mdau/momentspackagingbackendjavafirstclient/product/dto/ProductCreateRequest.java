package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String category;

    private String description;

    @NotNull(message = "MOQ is required")
    @Min(value = 1, message = "MOQ must be at least 1")
    private Integer moq = 1;

    private List<String> sizes = new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    private List<String> keywords = new ArrayList<>();

    private String primaryImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private Boolean isDiscount = false;

    private Integer discountPercent;

    private Boolean isNewArrival = false;

    private Boolean isFastMoving = false;

    private String material;

    private String finish;

    private List<UUID> industryIds = new ArrayList<>();
}