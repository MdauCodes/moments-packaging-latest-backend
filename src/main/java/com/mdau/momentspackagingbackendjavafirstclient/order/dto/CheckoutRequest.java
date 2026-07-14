package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.CourierType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FulfillmentType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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

    private String deliveryAddress;
    private String city;
    private String county;
    private String postalCode;
    private String notes;
    private String promoCode;
    private String sessionId;

    /** Sole Merchant rewards points to redeem against this order — optional, capped server-side. */
    @Min(value = 1, message = "redeemPoints must be positive")
    private Integer redeemPoints;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private FulfillmentType fulfillmentType = FulfillmentType.ZONE_DELIVERY;

    // ── OWN_COURIER fields ────────────────────────────────────────────────────

    private CourierType courierType;
    private String      courierServiceName;
    private String      courierStageOrOffice;

    // ── Idempotency ───────────────────────────────────────────────────────────

    /**
     * Client-generated UUID sent with every checkout attempt.
     * If the same key is received within 5 minutes, the existing order
     * is returned instead of creating a duplicate.
     * Frontend must generate this once per checkout session and persist
     * it until the order is confirmed.
     */
    private String idempotencyKey;

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