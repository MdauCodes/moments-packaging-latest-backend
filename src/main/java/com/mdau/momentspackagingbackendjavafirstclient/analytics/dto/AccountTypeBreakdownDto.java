package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;

public record AccountTypeBreakdownDto(String accountType, long customerCount, BigDecimal revenueKes) {
}
