package com.mdau.momentspackagingbackendjavafirstclient.cart.controller;

import com.mdau.momentspackagingbackendjavafirstclient.cart.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.cart.service.CartService;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService     cartService;
    private final RateLimitConfig rateLimitConfig;

    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.ok(cartService.getCart(user, sessionId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkCart(httpRequest);
        String resolvedSession = sessionId != null ? sessionId : request.getSessionId();
        return ResponseEntity.status(201)
                .body(cartService.addItem(user, resolvedSession, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(
                cartService.updateItemQuantity(user, sessionId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(cartService.removeItem(user, sessionId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        cartService.clearCart(user, sessionId);
        return ResponseEntity.noContent().build();
    }
}