package com.mdau.momentspackagingbackendjavafirstclient.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private AuthUserDto user;
}