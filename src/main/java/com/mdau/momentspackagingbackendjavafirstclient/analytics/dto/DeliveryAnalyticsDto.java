package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.time.Instant;
import java.util.List;

public record DeliveryAnalyticsDto(Instant rangeStart, Instant rangeEnd, List<DeliveryPerformanceDto> byFulfillmentType) {
}
