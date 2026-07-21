package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;

public record RewardsSourceBreakdownDto(String source, long coupons, BigDecimal valueKes) {
}
