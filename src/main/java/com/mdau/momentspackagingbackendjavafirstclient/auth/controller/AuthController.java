package com.mdau.momentspackagingbackendjavafirstclient.auth.controller;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.AuthResponse;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.LoginRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.RefreshRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.TokenResponse;
import com.mdau.momentspackagingbackendjavafirstclient.auth.service.AuthService;
import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitConfig rateLimitConfig;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimitConfig.checkLogin(httpRequest);
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}