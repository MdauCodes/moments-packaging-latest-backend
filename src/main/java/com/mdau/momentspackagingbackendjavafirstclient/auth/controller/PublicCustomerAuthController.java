package com.mdau.momentspackagingbackendjavafirstclient.auth.controller;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.auth.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PublicCustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody CustomerRegisterRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ResponseEntity.status(201).body(customerAuthService.register(request, sessionId));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(customerAuthService.verifyEmail(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        return ResponseEntity.ok(customerAuthService.resendOtp(request));
    }

    /** Step 1 — request reset code */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(customerAuthService.forgotPassword(request));
    }

    /** Step 2 — verify OTP, receive resetSessionToken */
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {
        return ResponseEntity.ok(customerAuthService.verifyResetOtp(request));
    }

    /** Step 3 — submit resetSessionToken + new password */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(customerAuthService.resetPassword(request));
    }
}