package com.mdau.momentspackagingbackendjavafirstclient.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.payment.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.payment.service.DarajaService;
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
    private final DarajaService   darajaService;
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
     * Daraja (M-Pesa direct) async callback — must return 200 regardless.
     * Safaricom retries on non-2xx for up to 3 minutes.
     *
     * {secret} is embedded in the CallBackURL we send with every STK push
     * request (DarajaService.buildCallbackUrl()) and validated here —
     * Safaricom's STK callback payload carries no signature we can verify,
     * so this is the practical equivalent of one. Without it, anyone who
     * knew/guessed a CheckoutRequestID could POST a fake "payment succeeded"
     * callback here and get an order marked paid without paying.
     */
    @PostMapping("/daraja/callback/{secret}")
    public ResponseEntity<String> handleDarajaCallback(
            @PathVariable String secret,
            @RequestBody String rawJson) {
        log.info("Daraja callback received");
        if (!darajaService.isValidCallbackSecret(secret)) {
            log.warn("Daraja callback rejected — invalid callback secret");
            return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Received\"}");
        }
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