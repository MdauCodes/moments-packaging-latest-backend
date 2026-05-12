package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.Cart;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.service.CartService;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import com.mdau.momentspackagingbackendjavafirstclient.notification.service.NotificationService;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.CheckoutRequest;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.*;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FulfillmentType;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.FulfillmentType;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

    @Transactional
    public OrderDto checkout(User customer, String sessionId, CheckoutRequest request) {
        Cart cart = cartService.getOrCreateCart(customer, sessionId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) throw new IllegalArgumentException("Cart is empty");

        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getLineTotalSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FulfillmentType fulfillmentType = request.getFulfillmentType() != null
                ? request.getFulfillmentType() : FulfillmentType.ZONE_DELIVERY;

        if (fulfillmentType == FulfillmentType.PICKUP) {
            boolean pickupEnabled = Boolean.parseBoolean(
                    settingsService.getValue("fulfillment.pickup.enabled", "false"));
            if (!pickupEnabled) throw new IllegalArgumentException(
                    "Pickup is currently unavailable. Please select delivery.");
        }
        if (fulfillmentType == FulfillmentType.OWN_COURIER) {
            boolean ownCourierEnabled = Boolean.parseBoolean(
                    settingsService.getValue("fulfillment.own-courier.enabled", "false"));
            if (!ownCourierEnabled) throw new IllegalArgumentException(
                    "Own courier option is currently unavailable. Please select delivery.");
        }

        BigDecimal deliveryFee = fulfillmentType == FulfillmentType.ZONE_DELIVERY
                ? deliveryZoneService.getFeeForCounty(request.getCounty())
                : BigDecimal.ZERO;

        BigDecimal discount = BigDecimal.ZERO;
        String appliedPromo = null;
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            Map<String, Object> promoResult = promoCodeService.validateAndCalculate(
                    request.getPromoCode(), subtotal);
            if (Boolean.TRUE.equals(promoResult.get("valid"))) {
                discount = (BigDecimal) promoResult.get("discountAmount");
                appliedPromo = request.getPromoCode().toUpperCase();
                promoCodeService.incrementUsedCount(request.getPromoCode());
            }
        }

        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);
        String reference = referenceGenerator.generate();

        Order order = Order.builder()
                .reference(reference).customer(customer)
                .contactName(request.getContactName()).email(request.getEmail())
                .phone(request.getPhone()).deliveryAddress(request.getDeliveryAddress())
                .city(request.getCity()).county(request.getCounty())
                .postalCode(request.getPostalCode())
                .status(OrderStatus.PENDING_PAYMENT)
                .fulfillmentType(fulfillmentType)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal).deliveryFee(deliveryFee)
                .discount(discount).totalAmount(total)
                .notes(request.getNotes()).promoCode(appliedPromo)
                .build();

        List<OrderItem> orderItems = cartItems.stream()
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

        order.getItems().addAll(orderItems);
        order.getStatusHistory().add(OrderStatusHistory.builder()
                .order(order).toStatus(OrderStatus.PENDING_PAYMENT)
                .note("Order created")
                .changedBy(customer != null ? customer.getEmail() : "guest")
                .build());

        Order saved = orderRepository.save(order);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        notificationService.onOrderCreated(saved);

        log.info("Order created: {}", reference);
        return new OrderDto(saved);
    }
}






