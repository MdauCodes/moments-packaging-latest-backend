package com.mdau.momentspackagingbackendjavafirstclient.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.payment.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.PayHeroService;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService  paymentService;
    private final PayHeroService  payHeroService;
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper    objectMapper;

    /**
     * POST /api/v1/payments/initiate
     * Initiates a payment (STK push, bank transfer, or COD).
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkPayment(httpRequest);
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    /**
     * GET /api/v1/payments/status/{orderId}
     *
     * Returns a normalized PaymentStatusResponse. Frontend polls this.
     * Status is always one of: PROCESSING | SUCCESS | FAILED | NO_PAYMENT
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentStatusResponse> getStatus(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    /**
     * POST /api/v1/payments/payhero/callback
     * Receives async result from PayHero after user enters PIN.
     * Must return 200 OK regardless — PayHero retries on non-2xx.
     */
    @PostMapping("/payhero/callback")
    public ResponseEntity<String> handleCallback(
            @RequestBody String rawJson,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("PayHero callback received. Auth header present: {}", authHeader != null);
        log.info("PayHero callback raw payload: {}", rawJson);  // ADD THIS
        try {
            PayHeroCallbackDto callback = objectMapper.readValue(rawJson, PayHeroCallbackDto.class);
            paymentService.handleCallback(callback);
            return ResponseEntity.ok("Callback processed");
        } catch (Exception e) {
            log.error("Failed to process PayHero callback: {}", e.getMessage());
            return ResponseEntity.ok("Callback received");
        }
    }
}