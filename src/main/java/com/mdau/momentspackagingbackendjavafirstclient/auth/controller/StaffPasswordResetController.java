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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles staff-specific auth actions:
 * - PATCH /api/v1/auth/change-password  (authenticated — first-login forced change)
 * - POST  /api/v1/auth/staff/forgot-password  (public — staff forgot password)
 * - POST  /api/v1/auth/staff/reset-password   (public — staff reset with token)
 *
 * Note: customer forgot/reset password uses OTP via PublicCustomerAuthController.
 * Staff use a token-link flow via email instead.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class StaffPasswordResetController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService    emailService;
    private final RateLimitConfig rateLimitConfig;

    // In-memory token store: token -> {email, expiry}
    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();

    record TokenEntry(String email, Instant expiry) {}

    /**
     * POST /api/v1/auth/staff/forgot-password
     * Body: { "email": "staff@example.com" }
     * Always returns 200 regardless of whether email exists.
     */
    @PostMapping("/staff/forgot-password")
    public ResponseEntity<Map<String, String>> staffForgotPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        rateLimitConfig.checkLogin(request);

        String email = body.getOrDefault("email", "").trim().toLowerCase();

        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsStaff())) return;

            String token = UUID.randomUUID().toString().replace("-", "");
            Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
            tokenStore.put(token, new TokenEntry(email, expiry));
            tokenStore.entrySet().removeIf(e -> e.getValue().expiry().isBefore(Instant.now()));

            emailService.sendStaffPasswordResetTokenEmail(user, token);
            log.info("Password reset token sent to staff: {}", email);
        });

        return ResponseEntity.ok(Map.of(
            "message", "If that email belongs to a staff account, a reset link has been sent."));
    }

    /**
     * POST /api/v1/auth/staff/reset-password
     * Body: { "token": "abc123...", "newPassword": "NewPass123!" }
     */
    @PostMapping("/staff/reset-password")
    public ResponseEntity<Map<String, String>> staffResetPassword(
            @RequestBody Map<String, String> body) {

        String token       = body.getOrDefault("token", "").trim();
        String newPassword = body.getOrDefault("newPassword", "").trim();

        if (token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Token and new password are required."));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Password must be at least 8 characters."));
        }

        TokenEntry entry = tokenStore.get(token);
        if (entry == null || entry.expiry().isBefore(Instant.now())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Reset token is invalid or has expired."));
        }

        userRepository.findByEmailAndDeletedFalse(entry.email()).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setMustChangePassword(false);
            user.setTempPasswordExpiresAt(null);
            userRepository.save(user);
            tokenStore.remove(token);
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