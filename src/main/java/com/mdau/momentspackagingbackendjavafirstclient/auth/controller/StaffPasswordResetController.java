package com.mdau.momentspackagingbackendjavafirstclient.auth.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/staff")
@RequiredArgsConstructor
public class StaffPasswordResetController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService    emailService;
    private final RateLimitConfig rateLimitConfig;

    // token store: 6-digit code -> {email, expiry}
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    record TokenEntry(String email, Instant expiry) {}

    /**
     * POST /api/v1/auth/staff/forgot-password
     * Body: { "email": "staff@example.com" }
     * Sends a 6-digit OTP to the staff email. Always returns 200.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> staffForgotPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        rateLimitConfig.checkLogin(request);
        String email = body.getOrDefault("email", "").trim().toLowerCase();

        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsStaff())) return;

            // Generate 6-digit OTP
            String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Instant expiry = Instant.now().plus(15, ChronoUnit.MINUTES);
            tokenStore.put(otp, new TokenEntry(email, expiry));
            // Clean expired tokens
            tokenStore.entrySet().removeIf(e -> e.getValue().expiry().isBefore(Instant.now()));

            emailService.sendStaffPasswordResetOtpEmail(user, otp);
            log.info("Password reset OTP sent to staff: {}", email);
        });

        return ResponseEntity.ok(Map.of(
            "message", "If that email belongs to a staff account, a reset code has been sent."));
    }

    /**
     * POST /api/v1/auth/staff/reset-password
     * Body: { "otp": "123456", "newPassword": "NewPass123!" }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> staffResetPassword(
            @RequestBody Map<String, String> body) {

        String otp         = body.getOrDefault("otp", "").trim();
        String newPassword = body.getOrDefault("newPassword", "").trim();

        if (otp.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "OTP and new password are required."));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Password must be at least 8 characters."));
        }

        TokenEntry entry = tokenStore.get(otp);
        if (entry == null || entry.expiry().isBefore(Instant.now())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Reset code is invalid or has expired."));
        }

        userRepository.findByEmailAndDeletedFalse(entry.email()).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setMustChangePassword(false);
            user.setTempPasswordExpiresAt(null);
            userRepository.save(user);
            tokenStore.remove(otp);
            log.info("Password reset completed for: {}", entry.email());
        });

        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    /**
     * PATCH /api/v1/auth/change-password
     * Authenticated — staff change their own password on first login.
     * Body: { "newPassword": "NewPass123!" }
     */
    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User principal) {

        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Password must be at least 8 characters."));
        }

        userRepository.findById(principal.getId()).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setMustChangePassword(false);
            user.setTempPasswordExpiresAt(null);
            userRepository.save(user);
            log.info("Password changed for: {}", user.getEmail());
        });

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}