package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EnquiryItemRequest {

    private UUID   productId;
    private String productName;
    private String size;
    private String material;
    private String finish;
    private Integer quantity;
    private String notes;
}