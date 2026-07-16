package com.mdau.momentspackagingbackendjavafirstclient.devtools.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunItem;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunRequest;
import com.mdau.momentspackagingbackendjavafirstclient.devtools.dto.CheckoutDryRunResult;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.DeliveryZoneService;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.PromoCodeService;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.PayHeroService;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxInvoicePdfService;
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
    private final PayHeroService       payHeroService;
    private final OrderRepository      orderRepository;
    private final TaxInvoicePdfService taxInvoicePdfService;

    @Transactional(readOnly = true)
    public CheckoutDryRunResult dryRunCheckout(CheckoutDryRunRequest request) {
        List<String> warnings = new ArrayList<>();
        List<CheckoutDryRunItem> resolvedItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;

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
                    BigDecimal lineVat = lineTotal.subtract(lineTotal.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP));
                    taxableAmount = taxableAmount.add(lineTotal);
                    vatAmount = vatAmount.add(lineVat);
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

        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);

        return new CheckoutDryRunResult(resolvedItems, subtotal, deliveryFee, discount, taxableAmount, vatAmount, total, appliedPromo, warnings);
    }

    /**
     * Fires a real STK push via PayHero with a synthetic external_reference — no PaymentRecord or
     * Order is ever created, so the eventual callback (whatever the tester does on their phone)
     * finds no matching record and safely no-ops (see PaymentService.processPaymentResult).
     */
    public String testStkPush(String phone, BigDecimal amount) {
        String testReference = "DEV-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return payHeroService.initiateSTKPush(phone, amount, testReference);
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
}
