package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CustomerAnalyticsDto(
        Instant rangeStart,
        Instant rangeEnd,
        long newPayingCustomersInRange,
        BigDecimal newCustomerFirstOrderValueKes,
        List<AccountTypeBreakdownDto> byAccountType,
        List<TopCustomerDto> topCustomersByLifetimeValue
) {
}
