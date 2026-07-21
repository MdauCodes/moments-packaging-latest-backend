package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Analytics Phase 2 — order funnel and operational health for an explicit date range. */
public record OperationsSummaryDto(
        Instant rangeStart,
        Instant rangeEnd,

        long totalOrders,
        List<StatusCountDto> funnel,
        List<StatusDurationDto> avgTimeInStage,

        long cancelledOrders,
        double cancellationRatePercent,

        long distinctCustomerCount,
        long repeatCustomerCount,
        double repeatCustomerRatePercent,

        long refundRequestedCount,
        BigDecimal refundRequestedValue,
        long refundResolvedCount,
        double avgRefundResolutionHours
) {
}
