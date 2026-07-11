package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RedeemPreviewRequest {

    @NotNull @Min(1)
    private Integer points;

    @NotNull
    private BigDecimal orderTotal;
}
