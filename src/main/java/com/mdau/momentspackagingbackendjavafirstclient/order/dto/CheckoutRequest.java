package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.CourierType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FulfillmentType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CheckoutRequest {

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(07|\\+2547)\\d{8}$", message = "Phone must be a valid Kenyan number")
    private String phone;

    /**
     * Required for ZONE_DELIVERY and OWN_COURIER.
     * Optional (ignored) for PICKUP.
     */
    private String deliveryAddress;

    private String city;
    private String county;
    private String postalCode;
    private String notes;
    private String promoCode;
    private String sessionId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private FulfillmentType fulfillmentType = FulfillmentType.ZONE_DELIVERY;

    // ── OWN_COURIER fields ────────────────────────────────────────────────────

    /**
     * Required when fulfillmentType = OWN_COURIER.
     * The courier service the customer will use.
     */
    private CourierType courierType;

    /**
     * Free-text — used when courierType = OTHER, or to specify extra detail.
     * e.g. "Crossroads matatu — route 111"
     */
    private String courierServiceName;

    /**
     * Optional but encouraged when fulfillmentType = OWN_COURIER.
     * The specific stage or parcel office within Nairobi CBD.
     * e.g. "Machakos Country Bus Stage", "G4S Kimathi Street office"
     */
    private String courierStageOrOffice;

    private List<InlineItem> items;

    @Getter
    @Setter
    public static class InlineItem {
        private String     productId;
        private Integer    quantity;
        private String     size;
        private String     material;
        private String     finish;
        private BigDecimal unitPrice;
    }
}