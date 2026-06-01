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

    private UUID       id;
    private UUID       uomId;
    private String     uomCode;
    private String     uomName;

    @NotBlank(message = "Collection name is required e.g. Packet of 25, Carton of 50 packets")
    private String     collectionName;

    private String     uomDescription;

    @NotNull(message = "Quantity (pieces in this UOM) is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer    quantity;

    @NotNull(message = "Price per piece is required")
    private BigDecimal pricePerUnit;

    /**
     * Optional compare-at price per piece (shown struck-through).
     * Must be greater than pricePerUnit to be displayed on frontend.
     * Null = no compare-at price shown.
     */
    private BigDecimal originalPricePerUnit;

    /** Computed: pricePerUnit * quantity. */
    private BigDecimal collectionPrice;

    /** Computed: originalPricePerUnit * quantity. Null when no compare-at price. */
    private BigDecimal originalCollectionPrice;

    private Integer    sortOrder = 0;
    private Boolean    enabled   = true;

    // Legacy
    private Integer    minQuantity;
    private Integer    maxQuantity;

    public ProductPricingTierDto(ProductPricingTier tier) {
        this.id                     = tier.getId();
        this.collectionName         = tier.getCollectionName();
        this.uomDescription         = tier.getUomDescription();
        this.quantity               = tier.getQuantity();
        this.pricePerUnit           = tier.getPricePerUnit();
        this.originalPricePerUnit   = tier.getOriginalPricePerUnit();
        this.collectionPrice        = tier.getCollectionPrice();
        this.originalCollectionPrice= tier.getOriginalCollectionPrice();
        this.sortOrder              = tier.getSortOrder();
        this.enabled                = tier.getEnabled();
        this.minQuantity            = tier.getMinQuantity();
        this.maxQuantity            = tier.getMaxQuantity();
        if (tier.getUom() != null) {
            this.uomId   = tier.getUom().getId();
            this.uomCode = tier.getUom().getCode();
            this.uomName = tier.getUom().getName();
        }
    }
}