package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.PriceUnit;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    private Boolean individualSalesEnabled = true;

    private List<String> sizes    = new ArrayList<>();
    private List<String> tags     = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();

    private String primaryImageUrl;
    private List<String> imageUrls = new ArrayList<>();

    private Boolean isDiscount      = false;
    private Integer discountPercent;
    private Boolean isNewArrival    = false;
    private Boolean isFastMoving    = false;

    private String material;
    private String finish;

    /** Current active price. */
    private BigDecimal basePrice;

    /**
     * Optional compare-at price (shown struck-through).
     * Should be higher than basePrice. Null = not shown.
     */
    private BigDecimal originalBasePrice;

    private PriceUnit  priceUnit     = PriceUnit.PER_UNIT;
    private StockStatus stockStatus  = StockStatus.IN_STOCK;
    private Integer leadTimeDays     = 14;
    private Boolean customizable     = false;
    private Integer stockCount       = 0;
    private Integer lowStockThreshold = 10;

    private BigDecimal vatRate;
    private Boolean   vatExempt = false;

    private List<UUID>                  industryIds  = new ArrayList<>();
    private List<ProductPricingTierDto> pricingTiers = new ArrayList<>();

    /** Real Segment/Category/Subcategory classification — optional, can also be set later via Classify Products. */
    private UUID       subcategoryId;
    private List<UUID> tagIds = new ArrayList<>();
}