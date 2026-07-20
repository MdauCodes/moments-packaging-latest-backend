package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
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
    private final String     invoiceNumber;
    private final Instant    paidAt;
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
    private final BigDecimal grossTaxableAmount;
    private final BigDecimal taxableAmount;
    private final BigDecimal vatAmount;
    private final BigDecimal deliveryFee;
    private final BigDecimal discount;
    private final BigDecimal totalAmount;
    private final Boolean    etrRequested;
    private final String     documentsEmail;
    private final String     notes;
    private final String     staffNotes;
    private final String     assignedTo;
    private final UUID       assignedToId;
    private final String     promoCode;
    private final String     fulfillmentType;

    // Dispatcher fields
    private final Boolean contentsVerified;
    private final String  deliveryConfirmationStatus;

    // OWN_COURIER fields — null for other fulfillment types
    private final String  courierType;
    private final String  courierServiceName;
    private final String  courierStageOrOffice;

    private final Instant refundRequestedAt;
    private final String  refundRequestReason;
    private final String  refundRequestedBy;
    private final Instant refundResolvedAt;

    private final List<OrderItemDto>          items;
    private final List<OrderStatusHistoryDto> statusHistory;
    private final Instant createdAt;
    private final Instant updatedAt;

    /** One-time secret for the frontend's Cloudinary tax-invoice upload flow — null unless taxInvoiceRequested. */
    @lombok.Setter
    private String taxInvoiceUploadToken;

    public OrderDto(Order order) {
        this.id                        = order.getId();
        this.reference                 = order.getReference();
        this.invoiceNumber             = order.getInvoiceNumber();
        this.paidAt                    = order.getPaidAt();
        this.status                    = order.getStatus().name();
        this.paymentMethod             = order.getPaymentMethod().name();
        this.paymentStatus             = order.getPaymentStatus().name();
        this.contactName               = order.getContactName();
        this.email                     = order.getEmail();
        this.phone                     = order.getPhone();
        this.deliveryAddress           = order.getDeliveryAddress();
        this.city                      = order.getCity();
        this.county                    = order.getCounty();
        this.postalCode                = order.getPostalCode();
        this.subtotal                  = order.getSubtotal();
        this.grossTaxableAmount        = order.getGrossTaxableAmount();
        this.taxableAmount             = order.getTaxableAmount();
        this.vatAmount                 = order.getVatAmount();
        this.deliveryFee               = order.getDeliveryFee();
        this.discount                  = order.getDiscount();
        this.totalAmount               = order.getTotalAmount();
        this.etrRequested              = order.getEtrRequested();
        this.documentsEmail            = order.getDocumentsEmail();
        this.notes                     = order.getNotes();
        this.staffNotes                = order.getStaffNotes();
        this.assignedTo                = order.getAssignedTo();
        this.assignedToId              = order.getAssignedToId();
        this.promoCode                 = order.getPromoCode();
        this.fulfillmentType           = order.getFulfillmentType() != null
                ? order.getFulfillmentType().name() : null;
        this.contentsVerified          = order.getContentsVerified();
        this.deliveryConfirmationStatus = order.getDeliveryConfirmationStatus() != null
                ? order.getDeliveryConfirmationStatus().name() : null;
        this.courierType               = order.getCourierType() != null
                ? order.getCourierType().name() : null;
        this.courierServiceName        = order.getCourierServiceName();
        this.courierStageOrOffice      = order.getCourierStageOrOffice();
        this.refundRequestedAt         = order.getRefundRequestedAt();
        this.refundRequestReason       = order.getRefundRequestReason();
        this.refundRequestedBy         = order.getRefundRequestedBy();
        this.refundResolvedAt          = order.getRefundResolvedAt();
        this.createdAt                 = order.getCreatedAt();
        this.updatedAt                 = order.getUpdatedAt();
        this.items                     = order.getItems().stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());
        this.statusHistory             = order.getStatusHistory().stream()
                .map(OrderStatusHistoryDto::new)
                .collect(Collectors.toList());
    }
}