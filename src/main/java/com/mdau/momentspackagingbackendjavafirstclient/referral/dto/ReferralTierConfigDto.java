package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralTierConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReferralTierConfigDto {
    private UUID       id;

    @NotBlank
    private String     tierName;

    @NotNull
    private BigDecimal minOrderAmount;

    private BigDecimal maxOrderAmount;

    @NotNull @Min(0)
    private Integer    referrerCredits;

    @NotNull @Min(0)
    private Integer    refereeCredits;

    private Boolean    isActive   = true;
    private Integer    sortOrder  = 0;

    public ReferralTierConfigDto(ReferralTierConfig t) {
        this.id              = t.getId();
        this.tierName        = t.getTierName();
        this.minOrderAmount  = t.getMinOrderAmount();
        this.maxOrderAmount  = t.getMaxOrderAmount();
        this.referrerCredits = t.getReferrerCredits();
        this.refereeCredits  = t.getRefereeCredits();
        this.isActive        = t.getIsActive();
        this.sortOrder       = t.getSortOrder();
    }
}