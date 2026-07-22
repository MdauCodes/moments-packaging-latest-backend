package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.time.Instant;
import java.util.List;

public record GeographicAnalyticsDto(Instant rangeStart, Instant rangeEnd, List<GeographicBreakdownDto> byCounty) {
}
