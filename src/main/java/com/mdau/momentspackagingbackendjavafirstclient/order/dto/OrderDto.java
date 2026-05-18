package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatusHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class OrderDto {
    private final UUID       id;
    private final String     reference;
    private final String     status;
    private final String     paymentMethod;
    private final String     paymentStatus;
    private final String     contactName;
    private final String     email;
    private final String     phone;
    private final String     deliveryAddress;
    private final String     city;
    private final String     county;
    private final String     postalCode;
    private final BigDecimal subtotal;
    private final BigDecimal deliveryFee;
    private final BigDecimal discount;
    private final BigDecimal totalAmount;
    private final String     notes;
    private final String     staffNotes;
    private final String     assignedTo;
    private final String     promoCode;
    private final String     fulfillmentType;

    // OWN_COURIER fields — null for other fulfillment types
    private final String     courierType;
    private final String     courierServiceName;
    private final String     courierStageOrOffice;

    private final List<OrderItemDto>          items;
    private final List<OrderStatusHistoryDto> statusHistory;
    private final Instant    createdAt;
    private final Instant    updatedAt;

    public OrderDto(Order order) {
        this.id                   = order.getId();
        this.reference            = order.getReference();
        this.status               = order.getStatus().name();
        this.paymentMethod        = order.getPaymentMethod().name();
        this.paymentStatus        = order.getPaymentStatus().name();
        this.contactName          = order.getContactName();
        this.email                = order.getEmail();
        this.phone                = order.getPhone();
        this.deliveryAddress      = order.getDeliveryAddress();
        this.city                 = order.getCity();
        this.county               = order.getCounty();
        this.postalCode           = order.getPostalCode();
        this.subtotal             = order.getSubtotal();
        this.deliveryFee          = order.getDeliveryFee();
        this.discount             = order.getDiscount();
        this.totalAmount          = order.getTotalAmount();
        this.notes                = order.getNotes();
        this.staffNotes           = order.getStaffNotes();
        this.assignedTo           = order.getAssignedTo();
        this.promoCode            = order.getPromoCode();
        this.fulfillmentType      = order.getFulfillmentType() != null
                ? order.getFulfillmentType().name() : null;
        this.courierType          = order.getCourierType() != null
                ? order.getCourierType().name() : null;
        this.courierServiceName   = order.getCourierServiceName();
        this.courierStageOrOffice = order.getCourierStageOrOffice();
        this.createdAt            = order.getCreatedAt();
        this.updatedAt            = order.getUpdatedAt();
        this.items                = order.getItems().stream()
                                        .map(OrderItemDto::new).collect(Collectors.toList());
        this.statusHistory        = order.getStatusHistory().stream()
                                        .map(OrderStatusHistoryDto::new).collect(Collectors.toList());
    }
}