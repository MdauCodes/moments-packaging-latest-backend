package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

public record StockAlertDto(String productName, int stockCount, int lowStockThreshold, String stockStatus) {
}
