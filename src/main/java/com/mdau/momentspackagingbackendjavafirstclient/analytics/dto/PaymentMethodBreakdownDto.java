package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

public record PaymentMethodBreakdownDto(
        String method,
        long successCount,
        long failedCount,
        long otherCount,
        double successRatePercent
) {
}
