package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductPricingTierDto {

    private UUID id;

    @NotBlank(message = "Collection name is required e.g. Half Dozen, Bale of 1000")
    private String collectionName;

    @NotNull(message = "Collection quantity is required")
    @Min(value = 1, message = "Collection quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price per unit is required")
    private BigDecimal pricePerUnit;

    /** Computed: pricePerUnit * quantity. Frontend displays this as the collection price. */
    private BigDecimal collectionPrice;

    private Integer sortOrder = 0;

    // Legacy fields — kept so existing data round-trips cleanly
    private Integer minQuantity;
    private Integer maxQuantity;

    public ProductPricingTierDto(ProductPricingTier tier) {
        this.id              = tier.getId();
        this.collectionName  = tier.getCollectionName();
        this.quantity        = tier.getQuantity();
        this.pricePerUnit    = tier.getPricePerUnit();
        this.collectionPrice = tier.getCollectionPrice();
        this.sortOrder       = tier.getSortOrder();
        this.minQuantity     = tier.getMinQuantity();
        this.maxQuantity     = tier.getMaxQuantity();
    }
}