package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;

public record TopCustomerDto(String name, String accountType, long lifetimeOrderCount, BigDecimal lifetimeRevenueKes) {
}
