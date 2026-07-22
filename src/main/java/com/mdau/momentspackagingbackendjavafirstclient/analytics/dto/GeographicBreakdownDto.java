package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;

public record GeographicBreakdownDto(String region, long orderCount, BigDecimal revenueKes) {
}
