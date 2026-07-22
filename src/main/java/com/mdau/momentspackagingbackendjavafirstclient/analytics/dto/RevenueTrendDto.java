package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.time.Instant;
import java.util.List;

public record RevenueTrendDto(Instant rangeStart, Instant rangeEnd, List<DailyRevenuePointDto> points) {
}
