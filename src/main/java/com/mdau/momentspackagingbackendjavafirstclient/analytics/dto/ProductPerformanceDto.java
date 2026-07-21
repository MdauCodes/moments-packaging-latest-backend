package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;

public record ProductPerformanceDto(String productName, long unitsSold, BigDecimal revenueKes) {
}
