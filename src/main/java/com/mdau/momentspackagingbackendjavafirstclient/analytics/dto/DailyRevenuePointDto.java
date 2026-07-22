package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenuePointDto(LocalDate date, BigDecimal paidKes, BigDecimal pendingKes, BigDecimal failedKes) {
}
