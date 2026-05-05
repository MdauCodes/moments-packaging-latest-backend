package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    IN_PRODUCTION,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
    REFUNDED
}