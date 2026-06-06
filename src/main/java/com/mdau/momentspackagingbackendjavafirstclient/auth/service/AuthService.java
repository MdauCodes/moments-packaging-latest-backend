package com.mdau.momentspackagingbackendjavafirstclient.auth.service;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.AuthResponse;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.ChangePasswordRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.AuthUserDto;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.LoginRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.RefreshRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.TokenResponse;
import com.mdau.momentspackagingbackendjavafirstclient.auth.entity.RefreshToken;
import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.common.security.JwtService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) auth.getPrincipal();

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        auditLogService.logSystem("AUTH", user.getId().toString(), user.getEmail(), "LOGIN", null, null);
        return new AuthResponse(accessToken, refreshToken, new AuthUserDto(user));
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (!stored.isValid()) {
            refreshTokenRepository.delete(stored);
            throw new ResourceNotFoundException("Refresh token is expired or revoked");
        }

        User user = stored.getUser();

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken  = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    auditLogService.logSystem("AUTH", rt.getUser().getId().toString(), rt.getUser().getEmail(), "LOGOUT", null, null);
                });
    }

    @Transactional
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        auditLogService.logSystem("AUTH", user.getId().toString(), userEmail, "PASSWORD_CHANGE", null, null);
        log.info("Password changed for user {}", userEmail);
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