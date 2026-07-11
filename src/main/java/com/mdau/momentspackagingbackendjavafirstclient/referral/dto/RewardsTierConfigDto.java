package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.RewardsTierConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RewardsTierConfigDto {
    private UUID    id;

    @NotBlank
    private String  tierName;

    @NotNull @Min(0)
    private Integer minLifetimePoints;

    @NotNull @Min(0)
    private Integer discountPercent;

    private String  perkDescription;

    private Boolean isActive  = true;
    private Integer sortOrder = 0;

    public RewardsTierConfigDto(RewardsTierConfig t) {
        this.id                = t.getId();
        this.tierName          = t.getTierName();
        this.minLifetimePoints = t.getMinLifetimePoints();
        this.discountPercent   = t.getDiscountPercent();
        this.perkDescription   = t.getPerkDescription();
        this.isActive          = t.getIsActive();
        this.sortOrder         = t.getSortOrder();
    }
}
