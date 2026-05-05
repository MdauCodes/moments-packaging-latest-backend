package com.mdau.momentspackagingbackendjavafirstclient.cart.service;

import com.mdau.momentspackagingbackendjavafirstclient.cart.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.Cart;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository  productRepository;
    private final PricingService     pricingService;

    // ── Get or create ────────────────────────────────────────────────

    @Transactional
    public Cart getOrCreateCart(User customer, String sessionId) {
        if (customer != null) {
            return cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                    .orElseGet(() -> cartRepository.save(
                            Cart.builder().customer(customer).build()));
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                    .orElseGet(() -> cartRepository.save(
                            Cart.builder().sessionId(sessionId).build()));
        }
        return cartRepository.save(Cart.builder().build());
    }

    // ── Read ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CartDto getCart(User customer, String sessionId) {
        Cart cart = resolveCart(customer, sessionId);
        if (cart == null) return emptyCart();
        return toDto(cart);
    }

    // ── Add item ─────────────────────────────────────────────────────

    @Transactional
    public CartDto addItem(User customer, String sessionId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customer, sessionId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));

        int quantity = request.getQuantity();

        if (quantity < product.getMoq()) {
            throw new IllegalArgumentException(
                    "Minimum order quantity for this product is " + product.getMoq());
        }

        // Check if same product + same config already in cart — increment qty
        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(existing -> {
                    int newQty = existing.getQuantity() + quantity;
                    BigDecimal unitPrice = pricingService.calculateUnitPrice(product, newQty);
                    existing.setQuantity(newQty);
                    existing.setUnitPriceSnapshot(unitPrice);
                    existing.setLineTotalSnapshot(
                            pricingService.calculateLineTotal(unitPrice, newQty));
                    cartItemRepository.save(existing);
                }, () -> {
                    BigDecimal unitPrice = pricingService.calculateUnitPrice(product, quantity);
                    CartItem item = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(quantity)
                            .unitPriceSnapshot(unitPrice)
                            .lineTotalSnapshot(
                                    pricingService.calculateLineTotal(unitPrice, quantity))
                            .productNameSnapshot(product.getName())
                            .sizeSnapshot(request.getSize())
                            .materialSnapshot(request.getMaterial())
                            .finishSnapshot(request.getFinish())
                            .build();
                    cartItemRepository.save(item);
                });

        return toDto(cart);
    }

    // ── Update quantity ───────────────────────────────────────────────

    @Transactional
    public CartDto updateItemQuantity(User customer, String sessionId,
                                      UUID itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCartOrThrow(customer, sessionId);

        CartItem item = cartItemRepository.findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        int newQty = request.getQuantity();
        Product product = item.getProduct();

        if (newQty < product.getMoq()) {
            throw new IllegalArgumentException(
                    "Minimum order quantity for this product is " + product.getMoq());
        }

        BigDecimal unitPrice = pricingService.calculateUnitPrice(product, newQty);
        item.setQuantity(newQty);
        item.setUnitPriceSnapshot(unitPrice);
        item.setLineTotalSnapshot(pricingService.calculateLineTotal(unitPrice, newQty));
        cartItemRepository.save(item);

        return toDto(cart);
    }

    // ── Remove item ───────────────────────────────────────────────────

    @Transactional
    public CartDto removeItem(User customer, String sessionId, UUID itemId) {
        Cart cart = resolveCartOrThrow(customer, sessionId);
        CartItem item = cartItemRepository.findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
        return toDto(cart);
    }

    // ── Clear cart ────────────────────────────────────────────────────

    @Transactional
    public void clearCart(User customer, String sessionId) {
        Cart cart = resolveCart(customer, sessionId);
        if (cart != null) {
            cartItemRepository.deleteByCartId(cart.getId());
        }
    }

    // ── Merge guest cart into customer cart ───────────────────────────

    @Transactional
    public void mergeGuestCart(String sessionId, User customer) {
        if (sessionId == null || sessionId.isBlank()) return;

        Cart guestCart = cartRepository
                .findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                .orElse(null);
        if (guestCart == null) return;

        Cart customerCart = cartRepository
                .findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    guestCart.setCustomer(customer);
                    guestCart.setSessionId(null);
                    return cartRepository.save(guestCart);
                });

        if (!customerCart.getId().equals(guestCart.getId())) {
            List<CartItem> guestItems = cartItemRepository.findByCartId(guestCart.getId());
            for (CartItem guestItem : guestItems) {
                cartItemRepository.findByCartIdAndProductId(
                        customerCart.getId(), guestItem.getProduct().getId())
                        .ifPresentOrElse(existing -> {
                            int newQty = existing.getQuantity() + guestItem.getQuantity();
                            BigDecimal unitPrice = pricingService.calculateUnitPrice(
                                    guestItem.getProduct(), newQty);
                            existing.setQuantity(newQty);
                            existing.setUnitPriceSnapshot(unitPrice);
                            existing.setLineTotalSnapshot(
                                    pricingService.calculateLineTotal(unitPrice, newQty));
                            cartItemRepository.save(existing);
                        }, () -> {
                            guestItem.setCart(customerCart);
                            cartItemRepository.save(guestItem);
                        });
            }
            guestCart.setStatus(CartStatus.ABANDONED);
            cartRepository.save(guestCart);
        }

        log.info("Guest cart merged for customer: {}", customer.getEmail());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Cart resolveCart(User customer, String sessionId) {
        if (customer != null) {
            return cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                    .orElse(null);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                    .orElse(null);
        }
        return null;
    }

    private Cart resolveCartOrThrow(User customer, String sessionId) {
        Cart cart = resolveCart(customer, sessionId);
        if (cart == null) throw new ResourceNotFoundException("Cart not found");
        return cart;
    }

    private CartDto toDto(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemDto> itemDtos = items.stream()
                .map(CartItemDto::new)
                .collect(Collectors.toList());
        BigDecimal subtotal = items.stream()
                .map(CartItem::getLineTotalSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartDto(cart.getId(), cart.getSessionId(),
                itemDtos, itemDtos.size(), subtotal, cart.getCreatedAt());
    }

    private CartDto emptyCart() {
        return new CartDto(null, null, List.of(), 0, BigDecimal.ZERO, null);
    }
}