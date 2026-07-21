package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TaxReportDto(
        Instant rangeStart,
        Instant rangeEnd,
        BigDecimal vatableSalesKes,
        BigDecimal vatToRemitKes,
        long paidOrderCount,
        long taxInvoiceRequestedCount,
        long etrRequestedCount,
        List<StatusCountDto> documentBundleStatusCounts
) {
}
