package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.CheckoutRequest;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.CheckoutService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final RateLimitConfig rateLimitConfig;

    @PostMapping
    public ResponseEntity<OrderDto> checkout(
            @AuthenticationPrincipal User user,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkCheckout(httpRequest);
        String resolvedSession = sessionId != null ? sessionId : request.getSessionId();
        return ResponseEntity.status(201)
                .body(checkoutService.checkout(user, resolvedSession, request));
    }
}