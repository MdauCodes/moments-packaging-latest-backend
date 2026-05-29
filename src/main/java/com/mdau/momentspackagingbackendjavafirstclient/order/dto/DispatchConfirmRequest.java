package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispatchConfirmRequest {
    /** Must match DeliveryConfirmationStatus enum values:
     *  CUSTOMER_PAYS_COURIER | CUSTOMER_PAYS_BUSINESS |
     *  REVERTED_TO_PICKUP    | CONFIRM_LATER
     */
    private String  deliveryConfirmationStatus;
    private Boolean contentsVerified;
}