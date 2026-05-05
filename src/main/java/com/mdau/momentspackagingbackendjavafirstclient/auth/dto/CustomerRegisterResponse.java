package com.mdau.momentspackagingbackendjavafirstclient.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomerRegisterResponse {
    private UUID   id;
    private String email;
    private String message;
}