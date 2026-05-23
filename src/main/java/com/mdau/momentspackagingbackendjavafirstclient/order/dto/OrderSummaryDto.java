package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lightweight order summary returned by the public email-lookup endpoint.
 * Deliberately excludes sensitive fields: staffNotes, assignedTo,
 * full deliveryAddress, idempotencyKey.
 * Phone is masked to last 4 digits.
 */
@Getter
public class OrderSummaryDto {

    private final UUID       id;
    private final String     reference;
    private final String     status;
    private final String     paymentStatus;
    private final String     paymentMethod;
    private final String     fulfillmentType;
    private final String     contactName;
    private final String     maskedEmail;
    private final String     maskedPhone;
    private final String     city;
    private final String     county;
    private final BigDecimal subtotal;
    private final BigDecimal deliveryFee;
    private final BigDecimal discount;
    private final BigDecimal totalAmount;
    private final String     promoCode;
    private final String     notes;

    // Courier info shown only for OWN_COURIER orders
    private final String     courierType;
    private final String     courierServiceName;
    private final String     courierStageOrOffice;

    private final List<OrderItemDto>          items;
    private final List<OrderStatusHistoryDto> statusHistory;
    private final Instant                     createdAt;
    private final Instant                     updatedAt;

    public OrderSummaryDto(Order order) {
        this.id                   = order.getId();
        this.reference            = order.getReference();
        this.status               = order.getStatus().name();
        this.paymentStatus        = order.getPaymentStatus().name();
        this.paymentMethod        = order.getPaymentMethod().name();
        this.fulfillmentType      = order.getFulfillmentType() != null
                ? order.getFulfillmentType().name() : null;
        this.contactName          = order.getContactName();
        this.maskedEmail          = maskEmail(order.getEmail());
        this.maskedPhone          = maskPhone(order.getPhone());
        this.city                 = order.getCity();
        this.county               = order.getCounty();
        this.subtotal             = order.getSubtotal();
        this.deliveryFee          = order.getDeliveryFee();
        this.discount             = order.getDiscount();
        this.totalAmount          = order.getTotalAmount();
        this.promoCode            = order.getPromoCode();
        this.notes                = order.getNotes();
        this.courierType          = order.getCourierType() != null
                ? order.getCourierType().name() : null;
        this.courierServiceName   = order.getCourierServiceName();
        this.courierStageOrOffice = order.getCourierStageOrOffice();
        // Items and status history are not loaded here (lazy) —
        // the frontend fetches full detail via /orders/track/{reference}
        this.items         = List.of();
        this.statusHistory = List.of();
        this.createdAt            = order.getCreatedAt();
        this.updatedAt            = order.getUpdatedAt();
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String visible = local.length() > 2
                ? local.substring(0, 2) + "***"
                : local.substring(0, 1) + "***";
        return visible + "@" + parts[1];
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}