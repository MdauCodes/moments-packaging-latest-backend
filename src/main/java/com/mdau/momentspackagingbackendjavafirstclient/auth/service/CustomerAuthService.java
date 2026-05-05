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

    @Value("${app.frontend.url:https://moments-connect-hub.lovable.app}")
    private String frontendUrl;

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

    @Transactional
    public AuthResponse verifyEmail(OtpVerifyRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        EmailVerificationToken evToken = evTokenRepository
                .findByTokenAndUsedFalse(request.getOtp())
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));

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

    @Transactional
    public Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plusSeconds(900))
                    .used(false)
                    .build();
            prTokenRepository.save(prt);
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user, resetLink);
        });
        return Map.of("message", "If an account exists, you will receive a reset link");
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = prTokenRepository
                .findByTokenAndUsedFalse(request.getToken())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        prTokenRepository.save(prt);

        log.info("Password reset for: {}", user.getEmail());
        return Map.of("message", "Password updated successfully");
    }

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