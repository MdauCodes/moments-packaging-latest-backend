package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderTrackingDto {
    private UUID   id;
    private String reference;
    private String status;
    private String contactName;
    private String maskedEmail;
    private List<TrackingItemDto> items;
    private List<TrackingHistoryDto> statusHistory;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;

    @Getter
    @AllArgsConstructor
    public static class TrackingItemDto {
        private String  productName;
        private Integer quantity;
        private BigDecimal lineTotal;
    }

    @Getter
    @AllArgsConstructor
    public static class TrackingHistoryDto {
        private String status;
        private Instant changedAt;
    }
}