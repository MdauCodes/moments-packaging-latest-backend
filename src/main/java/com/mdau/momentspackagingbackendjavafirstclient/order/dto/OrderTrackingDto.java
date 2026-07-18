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
    private String paymentStatus;
    private String paymentMethod;
    private String contactName;
    private String maskedEmail;
    private List<TrackingItemDto> items;
    private List<TrackingHistoryDto> statusHistory;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private String     fulfillmentType;
    // Financial breakdown — not PII, safe on the public tracking endpoint. Needed so a
    // customer's own "Download receipt" reconciles correctly from any device, not just
    // the one that placed the order (which has the fuller checkout-time record cached).
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal vatAmount;
    private String     invoiceNumber;
    private Instant    createdAt;
    private Instant    paidAt;
    private String     taxInvoiceKraPin;

    @Getter
    @AllArgsConstructor
    public static class TrackingItemDto {
        private String  productName;
        private Integer quantity;
        private BigDecimal lineTotal;
        // Not PII — needed so a receipt downloaded on a device without the checkout-time
        // local cache still shows real per-unit pricing instead of falling back to 0.
        private BigDecimal unitPrice;
        private String  size;
        private String  material;
        private String  finish;
    }

    @Getter
    @AllArgsConstructor
    public static class TrackingHistoryDto {
        private String status;
        private Instant changedAt;
    }
}
