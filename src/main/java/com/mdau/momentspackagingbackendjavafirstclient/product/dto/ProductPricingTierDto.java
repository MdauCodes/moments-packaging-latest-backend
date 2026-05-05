package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
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
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal pricePerUnit;

    public ProductPricingTierDto(ProductPricingTier tier) {
        this.id           = tier.getId();
        this.minQuantity  = tier.getMinQuantity();
        this.maxQuantity  = tier.getMaxQuantity();
        this.pricePerUnit = tier.getPricePerUnit();
    }
}