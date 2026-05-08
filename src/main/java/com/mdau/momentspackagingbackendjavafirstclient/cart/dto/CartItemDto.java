package com.mdau.momentspackagingbackendjavafirstclient.cart.dto;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class CartItemDto {
    private final UUID       id;
    private final UUID       productId;
    private final String     productName;
    private final String     primaryImageUrl;

    /** null = individual purchase */
    private final UUID       tierId;
    private final String     collectionName;
    private final Integer    collectionQuantity;

    /** Number of collections (or units for individual purchases) */
    private final Integer    quantity;

    /** Total physical units = quantity * collectionQuantity (or just quantity for individual) */
    private final Integer    totalUnits;

    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;
    private final String     size;
    private final String     material;
    private final String     finish;

    public CartItemDto(CartItem item) {
        this.id                 = item.getId();
        this.productId          = item.getProduct().getId();
        this.productName        = item.getProductNameSnapshot();
        this.primaryImageUrl    = item.getProduct().getPrimaryImageUrl();
        this.tierId             = item.getTier() != null ? item.getTier().getId() : null;
        this.collectionName     = item.getCollectionNameSnapshot();
        this.collectionQuantity = item.getCollectionQuantitySnapshot();
        this.quantity           = item.getQuantity();
        this.totalUnits         = item.getTotalUnits();
        this.unitPrice          = item.getUnitPriceSnapshot();
        this.lineTotal          = item.getLineTotalSnapshot();
        this.size               = item.getSizeSnapshot();
        this.material           = item.getMaterialSnapshot();
        this.finish             = item.getFinishSnapshot();
    }
}