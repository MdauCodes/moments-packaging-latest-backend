package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryItem;
import lombok.Getter;

import java.util.UUID;

@Getter
public class EnquiryItemDto {

    private final UUID   id;
    private final UUID   productId;
    private final String productName;
    private final String size;
    private final String material;
    private final String finish;
    private final Integer quantity;
    private final String notes;

    public EnquiryItemDto(EnquiryItem item) {
        this.id          = item.getId();
        this.productId   = item.getProductId();
        this.productName = item.getProductName();
        this.size        = item.getSize();
        this.material    = item.getMaterial();
        this.finish      = item.getFinish();
        this.quantity    = item.getQuantity();
        this.notes       = item.getNotes();
    }
}