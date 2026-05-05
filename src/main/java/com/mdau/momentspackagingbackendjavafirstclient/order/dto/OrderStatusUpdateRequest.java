package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusUpdateRequest {
    private String status;
    private String staffNotes;
}