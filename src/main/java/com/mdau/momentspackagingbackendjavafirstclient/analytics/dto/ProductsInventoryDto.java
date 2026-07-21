package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductsInventoryDto(
        Instant rangeStart,
        Instant rangeEnd,
        List<ProductPerformanceDto> topSellingByRevenue,
        long inStockCount,
        long lowStockCount,
        long outOfStockCount,
        BigDecimal totalInventoryCostValueKes,
        BigDecimal totalInventoryRetailValueKes,
        long productsMissingCostPriceCount,
        List<StockAlertDto> lowStockAlerts
) {
}
