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

    /** UUID of the ProductUom this tier belongs to. Optional for legacy tiers. */
    private UUID       uomId;

    /** UOM code for convenience e.g. "PACKET", "CARTON" */
    private String     uomCode;

    /** UOM display name e.g. "Packet", "Carton" */
    private String     uomName;

    @NotBlank(message = "Collection name is required e.g. Packet of 25, Carton of 50 packets")
    private String     collectionName;

    /**
     * Describes the UOM contents to the customer.
     * e.g. "A packet of 200ml tumblers has 25 pieces"
     */
    private String     uomDescription;

    @NotNull(message = "Quantity (pieces in this UOM) is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer    quantity;

    @NotNull(message = "Price per piece is required")
    private BigDecimal pricePerUnit;

    /** Computed: pricePerUnit * quantity. Displayed as the total UOM price. */
    private BigDecimal collectionPrice;

    private Integer    sortOrder = 0;

    /**
     * When false this UOM is hidden from public responses entirely.
     * Admins use this to disable inapplicable UOMs per product.
     */
    private Boolean    enabled   = true;

    // Legacy fields — kept for backward compat
    private Integer    minQuantity;
    private Integer    maxQuantity;

    public ProductPricingTierDto(ProductPricingTier tier) {
        this.id              = tier.getId();
        this.collectionName  = tier.getCollectionName();
        this.uomDescription  = tier.getUomDescription();
        this.quantity        = tier.getQuantity();
        this.pricePerUnit    = tier.getPricePerUnit();
        this.collectionPrice = tier.getCollectionPrice();
        this.sortOrder       = tier.getSortOrder();
        this.enabled         = tier.getEnabled();
        this.minQuantity     = tier.getMinQuantity();
        this.maxQuantity     = tier.getMaxQuantity();
        if (tier.getUom() != null) {
            this.uomId   = tier.getUom().getId();
            this.uomCode = tier.getUom().getCode();
            this.uomName = tier.getUom().getName();
        }
    }
}