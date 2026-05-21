package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

public enum DeliveryConfirmationStatus {
    PENDING_CONFIRMATION,      // dispatcher has not yet called customer
    CUSTOMER_PAYS_COURIER,     // customer pays courier directly
    CUSTOMER_PAYS_BUSINESS,    // business collects delivery fee and remits to courier
    REVERTED_TO_PICKUP,        // customer chose to pick up instead
    CONFIRM_LATER              // dispatcher will confirm after dispatch
}