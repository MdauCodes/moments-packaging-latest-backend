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

    /** Customer ticked "I need a tax invoice / VAT document" — see TaxDocumentService. */
    private boolean taxInvoiceRequested;

    /** Where to email the tax invoice PDF; defaults to `email` above if blank. */
    @Email(message = "Tax invoice email must be valid")
    private String taxInvoiceEmail;

    /**
     * Customer's own KRA PIN, printed on the tax invoice for their own remittance records.
     * Deliberately NOT validated here — this can arrive pre-filled from a Business Account's
     * saved profile, and a malformed value there must never block checkout/payment over a
     * cosmetic tax-document field. CheckoutService checks the format and silently drops it
     * if it doesn't match, rather than failing the whole order.
     */
    private String taxInvoiceKraPin;

    /** Customer ticked "Send me my ETR & tax documents". See Order.etrRequested. */
    private boolean etrRequested;

    /**
     * Where to email the receipt/tax-invoice/ETR bundle — required (checked server-side in
     * CheckoutService) only when etrRequested is true.
     */
    @Email(message = "Documents email must be valid")
    private String documentsEmail;

    /**
     * Reward Coupons to redeem against this order — optional, capped server-side. Allows 0 (not
     * just >=1) so any caller that sends "no redemption" as 0 rather than omitting the field
     * entirely doesn't fail checkout over it; CheckoutService already treats 0/null the same way.
     */
    @Min(value = 0, message = "redeemPoints cannot be negative")
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