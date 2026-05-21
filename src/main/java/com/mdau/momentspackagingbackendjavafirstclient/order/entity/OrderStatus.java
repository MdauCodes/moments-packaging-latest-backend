package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_VERIFIED,   // payments confirmer has verified the payment is correct
    IN_PRODUCTION,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}