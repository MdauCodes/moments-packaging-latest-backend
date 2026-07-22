package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

public record DeliveryPerformanceDto(
        String fulfillmentType,
        long totalOrders,
        long deliveredCount,
        long cancelledCount,
        double deliveryRatePercent,
        double avgDeliveryHours,
        long deliverySampleCount
) {
}
