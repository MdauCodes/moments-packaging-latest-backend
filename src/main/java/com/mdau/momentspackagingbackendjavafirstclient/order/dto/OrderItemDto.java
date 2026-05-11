package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class OrderItemDto {
    private final UUID       id;
    private final UUID       productId;
    private final String     productName;
    private final String     category;
    private final String     size;
    private final String     material;
    private final String     finish;
    private final String     collectionName;
    private final Integer    collectionQuantity;
    private final Integer    quantity;
    private final Integer    totalUnits;
    private final BigDecimal unitPrice;
    private final BigDecimal lineTotal;

    public OrderItemDto(OrderItem item) {
        this.id                  = item.getId();
        this.productId           = item.getProductId();
        this.productName         = item.getProductNameSnapshot();
        this.category            = item.getCategorySnapshot();
        this.size                = item.getSizeSnapshot();
        this.material            = item.getMaterialSnapshot();
        this.finish              = item.getFinishSnapshot();
        this.collectionName      = item.getCollectionNameSnapshot();
        this.collectionQuantity  = item.getCollectionQuantitySnapshot();
        this.quantity            = item.getQuantity();
        this.totalUnits          = item.getTotalUnits();
        this.unitPrice           = item.getUnitPrice();
        this.lineTotal           = item.getLineTotal();
    }
}