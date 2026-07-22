package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProfitabilityDto(
        Instant rangeStart,
        Instant rangeEnd,
        BigDecimal paidRevenueKes,
        BigDecimal estimatedCogsKes,
        BigDecimal estimatedGrossProfitKes,
        double grossMarginPercent,
        long unitsMissingCostPriceCount,
        BigDecimal couponRedemptionCostKes,
        BigDecimal estimatedNetProfitKes,
        double netMarginPercent
) {
}
