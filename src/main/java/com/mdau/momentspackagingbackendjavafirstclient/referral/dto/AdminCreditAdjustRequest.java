package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminCreditAdjustRequest {
    @NotNull
    private UUID   userId;
    @NotNull
    private Integer amount; // positive = add, negative = deduct
    @NotBlank
    private String  reason;
}