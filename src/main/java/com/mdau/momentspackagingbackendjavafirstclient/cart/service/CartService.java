package com.mdau.momentspackagingbackendjavafirstclient.cart.service;

import com.mdau.momentspackagingbackendjavafirstclient.cart.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.Cart;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartItemRepository;
import com.mdau.momentspackagingbackendjavafirstclient.cart.repository.CartRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductPricingTierRepository;
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

    private final CartRepository              cartRepository;
    private final CartItemRepository          cartItemRepository;
    private final ProductRepository           productRepository;
    private final ProductPricingTierRepository tierRepository;
    private final PricingService              pricingService;

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

    @Transactional(readOnly = true)
    public CartDto getCart(User customer, String sessionId) {
        Cart cart = resolveCart(customer, sessionId);
        if (cart == null) return emptyCart();
        return toDto(cart);
    }

    @Transactional
    public CartDto addItem(User customer, String sessionId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customer, sessionId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));

        int quantity = request.getQuantity();

        if (request.getTierId() != null) {
            // ── Collection purchase ──────────────────────────────────────────
            ProductPricingTier tier = tierRepository.findById(request.getTierId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pricing tier not found: " + request.getTierId()));

            if (!tier.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException(
                        "Tier does not belong to this product");
            }

            cartItemRepository.findByCartIdAndProductIdAndTierId(
                    cart.getId(), product.getId(), tier.getId())
                    .ifPresentOrElse(existing -> {
                        int newQty = existing.getQuantity() + quantity;
                        existing.setQuantity(newQty);
                        existing.setLineTotalSnapshot(
                                pricingService.collectionLineTotal(tier, newQty));
                        cartItemRepository.save(existing);
                    }, () -> {
                        BigDecimal lineTotal = pricingService.collectionLineTotal(tier, quantity);
                        CartItem item = CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .tier(tier)
                                .quantity(quantity)
                                .collectionNameSnapshot(tier.getCollectionName())
                                .collectionQuantitySnapshot(tier.getQuantity())
                                .unitPriceSnapshot(tier.getPricePerUnit())
                                .lineTotalSnapshot(lineTotal)
                                .productNameSnapshot(product.getName())
                                .sizeSnapshot(request.getSize())
                                .materialSnapshot(request.getMaterial())
                                .finishSnapshot(request.getFinish())
                                .build();
                        cartItemRepository.save(item);
                    });

        } else {
            // ── Individual unit purchase ─────────────────────────────────────
            if (!Boolean.TRUE.equals(product.getIndividualSalesEnabled())) {
                throw new IllegalArgumentException(
                        "This product is only available in collections. " +
                        "Please select a collection to purchase.");
            }

            if (quantity < product.getMoq()) {
                throw new IllegalArgumentException(
                        "Minimum order quantity for this product is " + product.getMoq());
            }

            cartItemRepository.findByCartIdAndProductIdAndTierIsNull(
                    cart.getId(), product.getId())
                    .ifPresentOrElse(existing -> {
                        int newQty = existing.getQuantity() + quantity;
                        BigDecimal unitPrice = product.getBasePrice() != null
                                ? product.getBasePrice() : BigDecimal.ZERO;
                        existing.setQuantity(newQty);
                        existing.setUnitPriceSnapshot(unitPrice);
                        existing.setLineTotalSnapshot(
                                pricingService.calculateLineTotal(unitPrice, newQty));
                        cartItemRepository.save(existing);
                    }, () -> {
                        BigDecimal unitPrice = product.getBasePrice() != null
                                ? product.getBasePrice() : BigDecimal.ZERO;
                        CartItem item = CartItem.builder()
                                .cart(cart)
                                .product(product)
                                .tier(null)
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
        }

        return toDto(cart);
    }

    @Transactional
    public CartDto updateItemQuantity(User customer, String sessionId,
                                      UUID itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCartOrThrow(customer, sessionId);
        CartItem item = cartItemRepository.findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        int newQty = request.getQuantity();

        if (item.getTier() != null) {
            item.setQuantity(newQty);
            item.setLineTotalSnapshot(
                    pricingService.collectionLineTotal(item.getTier(), newQty));
        } else {
            Product product = item.getProduct();
            if (newQty < product.getMoq()) {
                throw new IllegalArgumentException(
                        "Minimum order quantity for this product is " + product.getMoq());
            }
            BigDecimal unitPrice = product.getBasePrice() != null
                    ? product.getBasePrice() : BigDecimal.ZERO;
            item.setQuantity(newQty);
            item.setUnitPriceSnapshot(unitPrice);
            item.setLineTotalSnapshot(pricingService.calculateLineTotal(unitPrice, newQty));
        }

        cartItemRepository.save(item);
        return toDto(cart);
    }

    @Transactional
    public CartDto removeItem(User customer, String sessionId, UUID itemId) {
        Cart cart = resolveCartOrThrow(customer, sessionId);
        CartItem item = cartItemRepository.findByCartIdAndId(cart.getId(), itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(item);
        return toDto(cart);
    }

    @Transactional
    public void clearCart(User customer, String sessionId) {
        Cart cart = resolveCart(customer, sessionId);
        if (cart != null) cartItemRepository.deleteByCartId(cart.getId());
    }

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
                if (guestItem.getTier() != null) {
                    cartItemRepository.findByCartIdAndProductIdAndTierId(
                            customerCart.getId(),
                            guestItem.getProduct().getId(),
                            guestItem.getTier().getId())
                            .ifPresentOrElse(existing -> {
                                int newQty = existing.getQuantity() + guestItem.getQuantity();
                                existing.setQuantity(newQty);
                                existing.setLineTotalSnapshot(
                                        pricingService.collectionLineTotal(guestItem.getTier(), newQty));
                                cartItemRepository.save(existing);
                            }, () -> {
                                guestItem.setCart(customerCart);
                                cartItemRepository.save(guestItem);
                            });
                } else {
                    cartItemRepository.findByCartIdAndProductIdAndTierIsNull(
                            customerCart.getId(), guestItem.getProduct().getId())
                            .ifPresentOrElse(existing -> {
                                int newQty = existing.getQuantity() + guestItem.getQuantity();
                                BigDecimal unitPrice = existing.getUnitPriceSnapshot();
                                existing.setQuantity(newQty);
                                existing.setLineTotalSnapshot(
                                        pricingService.calculateLineTotal(unitPrice, newQty));
                                cartItemRepository.save(existing);
                            }, () -> {
                                guestItem.setCart(customerCart);
                                cartItemRepository.save(guestItem);
                            });
                }
            }
            guestCart.setStatus(CartStatus.ABANDONED);
            cartRepository.save(guestCart);
        }

        log.info("Guest cart merged for customer: {}", customer.getEmail());
    }

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