package com.mdau.momentspackagingbackendjavafirstclient.order.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderSummaryDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.dto.OrderTrackingDto;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService      orderService;
    private final OrderRepository   orderRepository;
    private final RateLimitConfig   rateLimitConfig;

    /**
     * Public order tracking by reference — no auth required.
     * GET /api/v1/orders/track/{reference}[?email=buyer@example.com]
     *
     * References are sequential (ORD-2026-07-0001, -0002, ...), not a secret, so reference
     * alone only returns status/progress — full details (financials, contact name, delivery
     * address) require the email param to match the order's own email.
     */
    @GetMapping("/track/{reference}")
    public ResponseEntity<OrderTrackingDto> track(
            @PathVariable String reference,
            @RequestParam(required = false) String email,
            HttpServletRequest request) {
        // The email param is a match/no-match oracle — rate-limit it the same as /by-email
        // to stop someone guessing emails against a known reference.
        if (email != null && !email.isBlank()) {
            rateLimitConfig.checkEmailLookup(request);
        }
        return ResponseEntity.ok(orderService.getTrackingInfo(reference, email));
    }

    /**
     * Public email-based order history lookup — no auth required.
     * GET /api/v1/orders/by-email?email=buyer@example.com&page=0&size=10
     *
     * Security:
     * - Rate limited: 10 requests/minute per IP
     * - Returns empty list (not 404) for unknown emails — prevents enumeration
     * - Sensitive fields masked: email shown as partial, phone last 4 digits only
     * - staffNotes, assignedTo, idempotencyKey never returned
     */
    @GetMapping("/by-email")
    public ResponseEntity<PageResponse<OrderSummaryDto>> getByEmail(
            @RequestParam String email,
            @PageableDefault(size = 10, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        rateLimitConfig.checkEmailLookup(request);

        int size = Math.min(pageable.getPageSize(), 20);
        Pageable capped = PageRequest.of(
                pageable.getPageNumber(), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(new PageResponse<>(
                orderRepository
                        .findByEmailIgnoreCaseOrderByCreatedAtDesc(email.trim(), capped)
                        .map(OrderSummaryDto::new)));
    }
}