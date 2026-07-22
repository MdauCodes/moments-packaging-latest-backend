package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A run-rate projection: actuals from the elapsed days of the current month (capped at the first
 * 7 days, per how the business wants this read — "as per first week of the month"), scaled up to
 * the month's full day count. Not a forecast model — a linear extrapolation, and labelled as such.
 */
public record MonthlyProjectionDto(
        Instant monthStart,
        Instant monthEnd,
        Instant sampleStart,
        Instant sampleEnd,
        int sampleDays,
        int daysInMonth,
        BigDecimal sampleRevenueKes,
        BigDecimal projectedRevenueKes,
        BigDecimal sampleGrossProfitKes,
        BigDecimal projectedGrossProfitKes,
        BigDecimal sampleCostsKes,
        BigDecimal projectedCostsKes
) {
}
