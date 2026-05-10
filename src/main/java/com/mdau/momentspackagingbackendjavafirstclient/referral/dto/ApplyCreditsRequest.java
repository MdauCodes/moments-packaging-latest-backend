package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ApplyCreditsRequest {
    @NotNull
    private UUID    orderId;
    @NotNull @Min(1)
    private Integer creditsToRedeem;
}