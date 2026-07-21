package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Phase 1 of the comprehensive analytics dashboard — revenue and payment health for an
 * explicit date range. paidRevenue is the only figure that should ever be labelled "revenue"
 * anywhere in the UI; pending/failed/refunded are shown alongside it precisely so they're
 * never mistaken for money actually received.
 */
public record RevenueSummaryDto(
        Instant rangeStart,
        Instant rangeEnd,

        BigDecimal paidRevenue,
        long paidOrderCount,

        BigDecimal pendingPaymentValue,
        long pendingOrderCount,

        BigDecimal failedPaymentValue,
        long failedOrderCount,

        BigDecimal refundedValue,
        long refundedOrderCount,

        BigDecimal averageOrderValue,

        List<PaymentMethodBreakdownDto> byMethod
) {
}
