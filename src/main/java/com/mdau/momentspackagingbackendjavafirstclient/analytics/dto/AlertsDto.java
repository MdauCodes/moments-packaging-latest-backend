package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.time.Instant;

public record AlertsDto(
        Instant checkedAt,
        long stalePendingOrders,
        long failedPaymentsRecent,
        long lowStockCount,
        long outOfStockCount,
        long unresolvedRefunds
) {
}
