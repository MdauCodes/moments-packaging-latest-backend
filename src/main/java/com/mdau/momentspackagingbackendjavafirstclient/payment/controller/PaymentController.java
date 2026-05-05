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

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            HttpServletRequest httpRequest) {
        rateLimitConfig.checkPayment(httpRequest);
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentInitiateResponse> getStatus(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    @PostMapping("/payhero/callback")
    public ResponseEntity<String> handleCallback(
            @RequestBody String rawJson,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("PayHero callback received. Auth header present: {}", authHeader != null);
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