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
    private final Integer    quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;
    private final String     size;
    private final String     material;
    private final String     finish;

    public CartItemDto(CartItem item) {
        this.id              = item.getId();
        this.productId       = item.getProduct().getId();
        this.productName     = item.getProductNameSnapshot();
        this.primaryImageUrl = item.getProduct().getPrimaryImageUrl();
        this.quantity        = item.getQuantity();
        this.unitPrice       = item.getUnitPriceSnapshot();
        this.lineTotal       = item.getLineTotalSnapshot();
        this.size            = item.getSizeSnapshot();
        this.material        = item.getMaterialSnapshot();
        this.finish          = item.getFinishSnapshot();
    }
}