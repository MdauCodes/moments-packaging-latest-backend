package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.Cart;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.service.CartService;
import com.mdau.momentspackagingbackendjavafirstclient.notification.service.NotificationService;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.CheckoutRequest;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FulfillmentType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderItem;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatusHistory;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderStatusHistoryRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.referral.service.ReferralService;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.MockModeService;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.service.TaxDocumentService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService             cartService;
    private final CartRepository          cartRepository;
    private final CartItemRepository      cartItemRepository;
    private final OrderRepository         orderRepository;
    private final OrderReferenceGenerator referenceGenerator;
    private final DeliveryZoneService     deliveryZoneService;
    private final NotificationService     notificationService;
    private final PromoCodeService        promoCodeService;
    private final SettingsService         settingsService;
    private final ProductRepository       productRepository;
    private final CacheManager            cacheManager;
    private final ReferralService         referralService;
    private final TaxDocumentService      taxDocumentService;

    @Transactional
    public OrderDto checkout(User customer, String sessionId, CheckoutRequest request) {

        // â”€â”€ Idempotency check â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // If client sends a key and we already processed it, return existing order.
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Cache idempCache = cacheManager.getCache("checkout-idempotency");
            if (idempCache != null) {
                Cache.ValueWrapper cached = idempCache.get(request.getIdempotencyKey());
                if (cached != null) {
                    String existingReference = (String) cached.get();
                    log.info("Idempotent checkout hit for key={} â†’ returning order {}",
                            request.getIdempotencyKey(), existingReference);
                    return orderRepository.findByReference(existingReference)
                            .map(OrderDto::new)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Idempotency cache points to missing order: " + existingReference));
                }
            }
        }

        // â”€â”€ Cart resolution â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Cart cart = cartService.getOrCreateCart(customer, sessionId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        final BigDecimal subtotal;

        if (!cartItems.isEmpty()) {
            subtotal = cartItems.stream()
                    .map(CartItem::getLineTotalSnapshot)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else if (request.getItems() != null && !request.getItems().isEmpty()) {
            log.info("Backend cart empty for session {}, falling back to {} inline items",
                    sessionId, request.getItems().size());
            subtotal = request.getItems().stream()
                    .map(it -> {
                        BigDecimal price = it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO;
                        int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                        return price.multiply(BigDecimal.valueOf(qty));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Guards against a zero-value order ever reaching payment — e.g. a lost auth context
        // mid-checkout falling back to inline request.getItems() without a unitPrice, or any other
        // pricing-resolution failure. A real product always costs more than KES 0; silently
        // proceeding here would create an order Daraja's own amount validation happens to reject,
        // but that's luck, not a guarantee, and the customer sees a confusing "Invalid Amount"
        // error with no idea why. Fail clearly instead.
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "We couldn't price your order correctly — please refresh the page and try again. " +
                    "If this keeps happening, contact us with your cart contents.");
        }

        // â”€â”€ Fulfillment validation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        FulfillmentType fulfillmentType = request.getFulfillmentType() != null
                ? request.getFulfillmentType() : FulfillmentType.ZONE_DELIVERY;

        if (fulfillmentType == FulfillmentType.PICKUP) {
            boolean pickupEnabled = Boolean.parseBoolean(
                    settingsService.getValue("fulfillment.pickup.enabled", "true"));
            if (!pickupEnabled) throw new IllegalArgumentException(
                    "Pickup is currently unavailable. Please select delivery.");
        }

        if (fulfillmentType == FulfillmentType.OWN_COURIER) {
            if (request.getCourierType() == null && (
                    request.getCourierServiceName() == null ||
                    request.getCourierServiceName().isBlank())) {
                throw new IllegalArgumentException(
                        "Please select a courier service for your delivery.");
            }
        }

        // â”€â”€ Delivery fee â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (fulfillmentType == FulfillmentType.ZONE_DELIVERY && request.getCounty() != null) {
            deliveryFee = deliveryZoneService.getFeeForCounty(request.getCounty());
        }

        // ── ETR / documents bundle — email is mandatory only when the customer checked the box ──
        if (request.isEtrRequested()
                && (request.getDocumentsEmail() == null || request.getDocumentsEmail().isBlank())) {
            throw new IllegalArgumentException(
                    "Please provide a valid email to receive your receipt, tax invoice and ETR.");
        }

        // â”€â”€ Promo codes cannot be combined with a Reward Coupons redemption on the same order â”€â”€
        // Every promo code (including a Business Account's auto-issued welcome code) carries this
        // as an explicit written term, so it's enforced here rather than left to the frontend to
        // remember to hide the other option.
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()
                && request.getRedeemPoints() != null && request.getRedeemPoints() > 0) {
            throw new IllegalArgumentException(
                    "A promo code can't be combined with a Reward Coupons redemption on the same order. Remove one to continue.");
        }

        // â”€â”€ Promo code â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromo = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            Map<String, Object> promoResult = promoCodeService.validateAndCalculate(
                    request.getPromoCode(), subtotal, customer != null ? customer.getId() : null);
            if (Boolean.TRUE.equals(promoResult.get("valid"))) {
                discount = (BigDecimal) promoResult.get("discountAmount");
                appliedPromo = request.getPromoCode().toUpperCase();
                promoCodeService.incrementUsedCount(request.getPromoCode());
            }
        }

        // ── Rewards points redemption ───────────────────────────────────────
        Integer redeemPoints = request.getRedeemPoints();
        BigDecimal pointsDiscount = BigDecimal.ZERO;
        if (redeemPoints != null && redeemPoints > 0 && customer != null) {
            BigDecimal preliminaryTotal = subtotal.add(deliveryFee).subtract(discount);
            BigDecimal requestedDiscount = referralService.calculateRedemptionDiscount(customer, redeemPoints);
            BigDecimal maxRedeemable = referralService.calculateMaxRedeemableKes(preliminaryTotal);
            pointsDiscount = requestedDiscount.compareTo(maxRedeemable) > 0 ? maxRedeemable : requestedDiscount;
            discount = discount.add(pointsDiscount);
        } else {
            redeemPoints = null;
        }

        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);
        String reference = referenceGenerator.generate();

        // ── VAT breakdown ────────────────────────────────────────────────
        // Product.basePrice (and therefore every line total / subtotal derived
        // from it) is VAT-inclusive, so VAT is extracted out of each line
        // rather than added on top. The order's discount is allocated
        // proportionally across every line (by that line's share of subtotal)
        // BEFORE VAT is extracted, so VAT is only ever charged on what the
        // customer actually paid — never on the pre-discount sticker price.
        BigDecimal discountRatio = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? discount.divide(subtotal, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal grossTaxableAmount = BigDecimal.ZERO;
        BigDecimal taxableAmount = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;
        if (!cartItems.isEmpty()) {
            for (CartItem ci : cartItems) {
                Product product = ci.getProduct();
                BigDecimal lineTotal = ci.getLineTotalSnapshot();
                if (lineTotal == null || Boolean.TRUE.equals(product != null ? product.getVatExempt() : null)) continue;
                BigDecimal rate = (product != null && product.getVatRate() != null) ? product.getVatRate() : new BigDecimal("0.16");
                BigDecimal lineDiscount = lineTotal.multiply(discountRatio).setScale(2, RoundingMode.HALF_UP);
                BigDecimal discountedLineTotal = lineTotal.subtract(lineDiscount);
                BigDecimal lineVat = discountedLineTotal.subtract(discountedLineTotal.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP));
                grossTaxableAmount = grossTaxableAmount.add(lineTotal);
                taxableAmount = taxableAmount.add(discountedLineTotal);
                vatAmount = vatAmount.add(lineVat);
            }
        } else if (request.getItems() != null) {
            for (var it : request.getItems()) {
                BigDecimal price = it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO;
                int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(qty));
                Product product = null;
                try {
                    product = productRepository.findByIdAndDeletedFalse(UUID.fromString(it.getProductId())).orElse(null);
                } catch (IllegalArgumentException ignored) {
                    // non-UUID productId — treat as standard-rated, matches the item-resolution fallback below
                }
                if (Boolean.TRUE.equals(product != null ? product.getVatExempt() : null)) continue;
                BigDecimal rate = (product != null && product.getVatRate() != null) ? product.getVatRate() : new BigDecimal("0.16");
                BigDecimal lineDiscount = lineTotal.multiply(discountRatio).setScale(2, RoundingMode.HALF_UP);
                BigDecimal discountedLineTotal = lineTotal.subtract(lineDiscount);
                BigDecimal lineVat = discountedLineTotal.subtract(discountedLineTotal.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP));
                grossTaxableAmount = grossTaxableAmount.add(lineTotal);
                taxableAmount = taxableAmount.add(discountedLineTotal);
                vatAmount = vatAmount.add(lineVat);
            }
        }

        // â”€â”€ Build order â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Order order = Order.builder()
                .reference(reference)
                .idempotencyKey(request.getIdempotencyKey())
                .customer(customer)
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .deliveryAddress(request.getDeliveryAddress())
                .city(request.getCity())
                .county(request.getCounty())
                .postalCode(request.getPostalCode())
                .status(OrderStatus.PENDING_PAYMENT)
                .fulfillmentType(fulfillmentType)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal)
                .grossTaxableAmount(grossTaxableAmount)
                .taxableAmount(taxableAmount)
                .vatAmount(vatAmount)
                .deliveryFee(deliveryFee)
                .discount(discount)
                .totalAmount(total)
                .notes(request.getNotes())
                .promoCode(appliedPromo)
                .courierType(request.getCourierType())
                .courierServiceName(request.getCourierServiceName())
                .courierStageOrOffice(request.getCourierStageOrOffice())
                .taxInvoiceRequested(request.isEtrRequested())
                .taxInvoiceEmail(request.isEtrRequested() ? resolveDocumentsEmail(request) : null)
                .taxInvoiceKraPin(resolveTaxInvoiceKraPin(request))
                .etrRequested(request.isEtrRequested())
                .documentsEmail(request.isEtrRequested() ? resolveDocumentsEmail(request) : null)
                .build();

        // â”€â”€ Resolve items â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<OrderItem> resolvedItems;
        if (!cartItems.isEmpty()) {
            resolvedItems = cartItems.stream()
                    .map(ci -> {
                        int totalUnits = ci.getTier() != null && ci.getCollectionQuantitySnapshot() != null
                                ? ci.getQuantity() * ci.getCollectionQuantitySnapshot()
                                : ci.getQuantity();
                        return OrderItem.builder()
                                .order(order)
                                .productId(ci.getProduct().getId())
                                .productNameSnapshot(ci.getProductNameSnapshot())
                                .categorySnapshot(ci.getProduct().getCategory())
                                .sizeSnapshot(ci.getSizeSnapshot())
                                .materialSnapshot(ci.getMaterialSnapshot())
                                .finishSnapshot(ci.getFinishSnapshot())
                                .collectionNameSnapshot(ci.getCollectionNameSnapshot())
                                .collectionQuantitySnapshot(ci.getCollectionQuantitySnapshot())
                                .quantity(ci.getQuantity())
                                .totalUnits(totalUnits)
                                .unitPrice(ci.getUnitPriceSnapshot())
                                .lineTotal(ci.getLineTotalSnapshot())
                                .build();
                    })
                    .collect(Collectors.toList());
        } else {
            resolvedItems = request.getItems().stream()
                    .map(it -> {
                        UUID productId;
                        String productName = "Product";
                        String category = null;
                        try {
                            productId = UUID.fromString(it.getProductId());
                            Product p = productRepository.findByIdAndDeletedFalse(productId).orElse(null);
                            if (p != null) {
                                productName = p.getName();
                                category = p.getCategory();
                            }
                        } catch (IllegalArgumentException e) {
                            productId = UUID.randomUUID();
                        }
                        int qty = it.getQuantity() != null ? it.getQuantity() : 1;
                        BigDecimal unitPrice = it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO;
                        return OrderItem.builder()
                                .order(order)
                                .productId(productId)
                                .productNameSnapshot(productName)
                                .categorySnapshot(category)
                                .sizeSnapshot(it.getSize())
                                .materialSnapshot(it.getMaterial())
                                .finishSnapshot(it.getFinish())
                                .quantity(qty)
                                .totalUnits(qty)
                                .unitPrice(unitPrice)
                                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        resolvedItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(resolvedItems);
        log.info("Saving order {} with {} items", reference, order.getItems().size());
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order)
                .toStatus(OrderStatus.PENDING_PAYMENT)
                .note("Order created")
                .changedBy(customer != null ? customer.getEmail() : "guest")
                .build());

        Order saved = orderRepository.save(order);

        if (redeemPoints != null) {
            try {
                referralService.commitRedemption(customer, redeemPoints, pointsDiscount, saved);
            } catch (Exception e) {
                log.error("Points redemption commit failed for order {}: {}",
                        saved.getReference(), e.getMessage(), e);
            }
        }

        // â”€â”€ Store idempotency key â†’ reference â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Cache idempCache = cacheManager.getCache("checkout-idempotency");
            if (idempCache != null) {
                idempCache.put(request.getIdempotencyKey(), saved.getReference());
            }
        }

        if (!cartItems.isEmpty()) {
            cart.setStatus(CartStatus.CHECKED_OUT);
            cartRepository.save(cart);
        }

        notificationService.onOrderCreated(saved);

        OrderDto dto = new OrderDto(saved);

        if (Boolean.TRUE.equals(saved.getTaxInvoiceRequested())) {
            try {
                dto.setTaxInvoiceUploadToken(taxDocumentService.requestForOrder(saved).getUploadToken());
            } catch (Exception e) {
                // A tax-invoice generation hiccup must never take the whole checkout down —
                // it's retryable from the admin tax-documents tab once that exists (Phase 3).
                log.error("Tax invoice request failed for order {}: {}", saved.getReference(), e.getMessage(), e);
            }
        }

        log.info("Order created: {} fulfillment={} (source: {})",
                reference, fulfillmentType, cartItems.isEmpty() ? "inline" : "cart");
        return dto;
    }

    private String resolveDocumentsEmail(CheckoutRequest request) {
        return request.getDocumentsEmail().trim();
    }

    private static final java.util.regex.Pattern KRA_PIN_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z]\\d{9}[A-Za-z]$");

    /**
     * Deliberately lenient: this can arrive pre-filled from a Business Account's saved profile,
     * and a malformed value there must never block checkout/payment over a cosmetic tax-document
     * field. A bad format is dropped (order proceeds with no KRA PIN on the invoice) rather than
     * failing the whole request — see CheckoutRequest.taxInvoiceKraPin for why this isn't a
     * @Pattern constraint on the DTO instead.
     */
    private String resolveTaxInvoiceKraPin(CheckoutRequest request) {
        if (!request.isTaxInvoiceRequested()) return null;
        String raw = request.getTaxInvoiceKraPin();
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.trim().toUpperCase();
        if (!KRA_PIN_PATTERN.matcher(normalized).matches()) {
            log.warn("Dropping malformed taxInvoiceKraPin on checkout (order proceeds without it): \"{}\"", raw);
            return null;
        }
        return normalized;
    }
}