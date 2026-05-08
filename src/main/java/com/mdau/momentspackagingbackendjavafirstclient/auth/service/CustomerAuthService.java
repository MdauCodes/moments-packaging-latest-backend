package com.mdau.momentspackagingbackendjavafirstclient.auth.service;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.*;
import com.mdau.momentspackagingbackendjavafirstclient.auth.entity.EmailVerificationToken;
import com.mdau.momentspackagingbackendjavafirstclient.auth.entity.PasswordResetToken;
import com.mdau.momentspackagingbackendjavafirstclient.auth.entity.RefreshToken;
import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.EmailVerificationTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.PasswordResetTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.security.JwtService;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private final UserRepository                   userRepository;
    private final EmailVerificationTokenRepository evTokenRepository;
    private final PasswordResetTokenRepository     prTokenRepository;
    private final RefreshTokenRepository           refreshTokenRepository;
    private final JwtService                       jwtService;
    private final PasswordEncoder                  passwordEncoder;
    private final EmailService                     emailService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public CustomerRegisterResponse register(CustomerRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .emailVerified(false)
                .enabled(true)
                .roles(Set.of(Role.ROLE_CUSTOMER))
                .build();

        User saved = userRepository.save(user);

        String otp = generateOtp();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(saved)
                .token(otp)
                .expiresAt(Instant.now().plusSeconds(900))
                .used(false)
                .build();
        evTokenRepository.save(token);

        emailService.sendOtpEmail(saved, otp);

        log.info("Customer registered: {}", saved.getEmail());
        return new CustomerRegisterResponse(saved.getId(), saved.getEmail(),
                "Verification code sent to your email");
    }

    // ── Verify email ──────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse verifyEmail(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        EmailVerificationToken evToken = evTokenRepository
                .findByTokenAndUsedFalse(request.getOtp())
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired verification code"));

        evToken.setUsed(true);
        evTokenRepository.save(evToken);

        user.setEmailVerified(true);
        userRepository.save(user);

        emailService.sendWelcomeEmail(user);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("Email verified for: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken, new AuthUserDto(user));
    }

    // ── Resend OTP ────────────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> resendOtp(ResendOtpRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!user.getEmailVerified()) {
                evTokenRepository.findByUserAndUsedFalse(user).ifPresent(t -> {
                    t.setUsed(true);
                    evTokenRepository.save(t);
                });
                String otp = generateOtp();
                EmailVerificationToken token = EmailVerificationToken.builder()
                        .user(user)
                        .token(otp)
                        .expiresAt(Instant.now().plusSeconds(900))
                        .used(false)
                        .build();
                evTokenRepository.save(token);
                emailService.sendOtpEmail(user, otp);
            }
        });
        return Map.of("message", "Code sent if account exists");
    }

    // ── Forgot password — Step 1: send OTP ───────────────────────────────────

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // Invalidate ALL existing unused reset tokens for this user
            List<PasswordResetToken> existing = prTokenRepository.findAllByUserAndUsedFalse(user);
            existing.forEach(t -> t.setUsed(true));
            prTokenRepository.saveAll(existing);

            String otp = generateOtp();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(otp)
                    .expiresAt(Instant.now().plusSeconds(900))
                    .used(false)
                    .build();
            prTokenRepository.save(prt);

            emailService.sendPasswordResetOtpEmail(user, otp);
            log.info("Password reset OTP sent to {}", user.getEmail());
        });
        return Map.of("message", "If an account exists, you will receive a reset code");
    }

    // ── Verify reset OTP — Step 2: validate OTP, return session token ─────────

    @Transactional
    public Map<String, String> verifyResetOtp(VerifyResetOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        PasswordResetToken prt = prTokenRepository
                .findAllByUserAndUsedFalse(user)
                .stream()
                .filter(t -> t.getToken().equals(request.getOtp()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        // Replace OTP with a secure session token — valid for 10 minutes
        String sessionToken = UUID.randomUUID().toString();
        prt.setToken(sessionToken);
        prt.setExpiresAt(Instant.now().plusSeconds(600));
        prTokenRepository.save(prt);

        log.info("Reset OTP verified for {}, session token issued", user.getEmail());
        return Map.of(
                "resetSessionToken", sessionToken,
                "message", "OTP verified. Use the resetSessionToken to set your new password."
        );
    }

    // ── Reset password — Step 3: use session token to set new password ────────

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = prTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset session. Please request a new code."));

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        prTokenRepository.save(prt);

        log.info("Password reset successfully for: {}", user.getEmail());
        return Map.of("message", "Password updated successfully. You can now log in.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private String createRefreshToken(User user) {
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }
}