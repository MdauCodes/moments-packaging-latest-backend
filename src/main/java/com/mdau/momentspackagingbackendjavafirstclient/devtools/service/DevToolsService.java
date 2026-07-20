package com.mdau.momentspackagingbackendjavafirstclient.devtools.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunItem;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunRequest;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunResult;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.DeliveryZoneService;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.PromoCodeService;
import com.mdau.momentspackagingbackendjavafirstclient.payment.dto.DarajaCallbackDto;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.DarajaService;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.PaymentService;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxInvoicePdfService;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadResponse;
import com.mdau.momentspackagingbackendjavafirstclient.upload.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Backs the admin Developer Tools dashboard. Every method here is deliberately side-effect-free
 * against real customer/business data — no Order, PaymentRecord or Cart row is ever created or
 * touched. This is the one place in the codebase where that guarantee matters more than reusing
 * CheckoutService/PaymentService directly, since those are threaded through with persistence at
 * nearly every step.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevToolsService {

    private final ProductRepository    productRepository;
    private final DeliveryZoneService  deliveryZoneService;
    private final PromoCodeService     promoCodeService;
    private final DarajaService        darajaService;
    private final PaymentService       paymentService;
    private final OrderRepository      orderRepository;
    private final TaxInvoicePdfService taxInvoicePdfService;
    private final UploadService        uploadService;
    private final EmailService         emailService;

    private record PricedLine(BigDecimal lineTotal, BigDecimal vatRate) {}

    @Transactional(readOnly = true)
    public CheckoutDryRunResult dryRunCheckout(CheckoutDryRunRequest request) {
        List<String> warnings = new ArrayList<>();
        List<CheckoutDryRunItem> resolvedItems = new ArrayList<>();
        List<PricedLine> vatableLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        if (request.getItems() == null || request.getItems().isEmpty()) {
            warnings.add("No items provided — nothing to price.");
        } else {
            for (CheckoutDryRunRequest.Item it : request.getItems()) {
                int qty = it.getQuantity() != null && it.getQuantity() > 0 ? it.getQuantity() : 1;
                Product product = null;
                try {
                    product = productRepository.findByIdAndDeletedFalse(UUID.fromString(it.getProductId())).orElse(null);
                } catch (IllegalArgumentException ignored) {
                    warnings.add("Product ID '" + it.getProductId() + "' is not a valid UUID.");
                }
                if (product == null) {
                    warnings.add("Product " + it.getProductId() + " not found — priced at 0.");
                    resolvedItems.add(new CheckoutDryRunItem(it.getProductId(), "Unknown product", qty, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    continue;
                }
                if (Boolean.TRUE.equals(product.getRisellerSuspended())) {
                    warnings.add(product.getName() + " is currently suspended (out of catalog).");
                }
                if (product.getStockCount() != null && product.getStockCount() < qty
                        && !"MADE_TO_ORDER".equals(product.getStockStatus().name())) {
                    warnings.add(product.getName() + " has only " + product.getStockCount() + " in stock (requested " + qty + ").");
                }
                BigDecimal unitPrice = it.getUnitPrice() != null ? it.getUnitPrice() : product.getBasePrice();
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                subtotal = subtotal.add(lineTotal);
                resolvedItems.add(new CheckoutDryRunItem(it.getProductId(), product.getName(), qty, unitPrice, lineTotal, true));

                if (!Boolean.TRUE.equals(product.getVatExempt())) {
                    BigDecimal rate = product.getVatRate() != null ? product.getVatRate() : new BigDecimal("0.16");
                    vatableLines.add(new PricedLine(lineTotal, rate));
                }
            }
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (request.getCounty() != null && !request.getCounty().isBlank()) {
            try {
                deliveryFee = deliveryZoneService.getFeeForCounty(request.getCounty());
            } catch (Exception e) {
                warnings.add("Could not resolve delivery fee for county '" + request.getCounty() + "': " + e.getMessage());
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromo = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            var promoResult = promoCodeService.validateAndCalculate(request.getPromoCode(), subtotal);
            if (Boolean.TRUE.equals(promoResult.get("valid"))) {
                discount = (BigDecimal) promoResult.get("discountAmount");
                appliedPromo = request.getPromoCode().toUpperCase();
            } else {
                warnings.add("Promo code not applied: " + promoResult.getOrDefault("message", "invalid"));
            }
        }

        if (request.getRedeemPoints() != null && request.getRedeemPoints() > 0) {
            warnings.add("Points redemption preview isn't simulated here — it depends on a real customer's points balance.");
        }

        // Mirrors CheckoutService.checkout(): the discount is allocated proportionally across
        // every line (by share of subtotal) before VAT is extracted, so this preview doesn't
        // drift from the real checkout math.
        BigDecimal discountRatio = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? discount.divide(subtotal, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;
        for (PricedLine line : vatableLines) {
            BigDecimal lineDiscount = line.lineTotal().multiply(discountRatio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountedLineTotal = line.lineTotal().subtract(lineDiscount);
            BigDecimal lineVat = discountedLineTotal.subtract(discountedLineTotal.divide(BigDecimal.ONE.add(line.vatRate()), 2, RoundingMode.HALF_UP));
            taxableAmount = taxableAmount.add(discountedLineTotal);
            vatAmount = vatAmount.add(lineVat);
        }

        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);

        return new CheckoutDryRunResult(resolvedItems, subtotal, deliveryFee, discount, taxableAmount, vatAmount, total, appliedPromo, warnings);
    }

    /**
     * Fires a real STK push via Daraja (the live M-Pesa integration — there is no PayHero
     * fallback anymore) with a synthetic external_reference. No PaymentRecord or Order is ever
     * created, so the eventual real Safaricom callback (whatever the tester does on their phone)
     * finds no matching record and safely no-ops (see PaymentService.processPaymentResult).
     * Returns the real Daraja CheckoutRequestID so the caller can also exercise
     * simulateDarajaCallback() against it.
     */
    public String testStkPush(String phone, BigDecimal amount) {
        String testReference = "DEV-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return darajaService.initiateSTKPush(phone, amount, testReference);
    }

    /**
     * Builds and feeds a synthetic Daraja STK callback payload straight into
     * PaymentService.handleDarajaCallback — the exact same code path Safaricom's real callback
     * hits — without going through the public HTTP endpoint or its secret-path check. Since no
     * PaymentRecord was ever created for a dev-tools checkoutRequestId, this always resolves to
     * the "no matching record" no-op branch: it proves the callback handler parses the payload
     * and returns cleanly for both success and failure result codes without writing to any real
     * Order/PaymentRecord.
     */
    public String simulateDarajaCallback(String checkoutRequestId, boolean success) {
        DarajaCallbackDto callback = new DarajaCallbackDto();
        DarajaCallbackDto.Body body = new DarajaCallbackDto.Body();
        DarajaCallbackDto.StkCallback stk = new DarajaCallbackDto.StkCallback();
        stk.setCheckoutRequestId(checkoutRequestId);
        stk.setMerchantRequestId("DEV-TEST-MERCHANT-REQ");
        if (success) {
            stk.setResultCode(0);
            stk.setResultDesc("The service request is processed successfully.");
            DarajaCallbackDto.CallbackMetadata metadata = new DarajaCallbackDto.CallbackMetadata();
            DarajaCallbackDto.MetadataItem receiptItem = new DarajaCallbackDto.MetadataItem();
            receiptItem.setName("MpesaReceiptNumber");
            receiptItem.setValue("DEV" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            metadata.setItem(List.of(receiptItem));
            stk.setCallbackMetadata(metadata);
        } else {
            stk.setResultCode(1032);
            stk.setResultDesc("Request cancelled by user.");
        }
        body.setStkCallback(stk);
        callback.setBody(body);

        paymentService.handleDarajaCallback(callback);
        return "Callback processed for checkoutRequestId=" + checkoutRequestId
                + " (resultCode=" + stk.getResultCode() + "). No PaymentRecord existed for this "
                + "dev-tools push, so PaymentService safely logged a \"no matching record\" "
                + "warning and made no database changes — that's the expected, correct behavior.";
    }

    /**
     * Renders the tax-invoice PDF for a real, already-placed order and returns the raw bytes —
     * bypasses TaxDocumentService entirely, so nothing is uploaded to Cloudinary and no email is
     * sent. Purely for previewing what the PDF looks like.
     */
    @Transactional(readOnly = true)
    public byte[] previewTaxInvoice(String orderReference) {
        Order order = orderRepository.findByReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderReference));
        return taxInvoicePdfService.render(order);
    }

    /**
     * Renders the tax-invoice PDF for a real, already-placed order and actually emails it to
     * whatever test address is given — the one part of the "email/PDF" flow that previewTaxInvoice()
     * above can't exercise, since that method deliberately never touches Cloudinary or email.
     * Uploads to a distinct "dev-test" Cloudinary folder (never the real "tax-documents" one) and
     * never creates or touches a TaxDocument row, so it can't be confused with (or interfere with)
     * a real customer's tax invoice history.
     */
    @Transactional(readOnly = true)
    public String sendTestTaxInvoiceEmail(String orderReference, String testEmail) {
        Order order = orderRepository.findByReference(orderReference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderReference));
        byte[] pdfBytes = taxInvoicePdfService.render(order);
        String filename = "dev-test-tax-invoice-" + orderReference + "-" + System.currentTimeMillis() + ".pdf";
        UploadResponse uploaded = uploadService.uploadRaw(pdfBytes, "dev-test", filename);

        TaxDocument transientDoc = TaxDocument.builder()
                .order(order)
                .recipientEmail(testEmail)
                .cloudinaryUrl(uploaded.getUrl())
                .cloudinaryPublicId(uploaded.getPublicId())
                .build();
        try {
            emailService.sendTaxInvoiceReadyEmail(transientDoc, null);
        } catch (Exception e) {
            throw new RuntimeException("Test email failed: " + e.getMessage(), e);
        }
        return "Test tax invoice email sent to " + testEmail + " for order " + orderReference;
    }
}
