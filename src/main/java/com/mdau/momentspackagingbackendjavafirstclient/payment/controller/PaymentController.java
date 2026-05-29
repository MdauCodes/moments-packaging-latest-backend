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
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    /**
     * PayHero async callback — must return 200 regardless.
     */
    @PostMapping("/payhero/callback")
    public ResponseEntity<String> handlePayHeroCallback(
            @RequestBody String rawJson,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("PayHero callback received. Auth header present: {}", authHeader != null);
        log.debug("PayHero callback raw payload: {}", rawJson);
        try {
            PayHeroCallbackDto callback = objectMapper.readValue(rawJson, PayHeroCallbackDto.class);
            paymentService.handleCallback(callback);
            return ResponseEntity.ok("Callback processed");
        } catch (Exception e) {
            log.error("Failed to process PayHero callback: {}", e.getMessage());
            return ResponseEntity.ok("Callback received");
        }
    }

    /**
     * Daraja (M-Pesa direct) async callback — must return 200 regardless.
     * Safaricom retries on non-2xx for up to 3 minutes.
     */
    @PostMapping("/daraja/callback")
    public ResponseEntity<String> handleDarajaCallback(@RequestBody String rawJson) {
        log.info("Daraja callback received");
        log.debug("Daraja callback raw payload: {}", rawJson);
        try {
            DarajaCallbackDto callback = objectMapper.readValue(rawJson, DarajaCallbackDto.class);
            paymentService.handleDarajaCallback(callback);
            return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Success\"}");
        } catch (Exception e) {
            log.error("Failed to process Daraja callback: {}", e.getMessage());
            return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Received\"}");
        }
    }
}