package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReferralCodeDto {
    private final UUID    id;
    private final String  code;
    private final String  referralUrl;
    private final Boolean isActive;
    private final Integer totalReferrals;
    private final Integer maxReferrals;
    private final Instant createdAt;

    public ReferralCodeDto(ReferralCode rc, String frontendUrl) {
        this.id             = rc.getId();
        this.code           = rc.getCode();
        this.referralUrl    = frontendUrl + "/register?ref=" + rc.getCode();
        this.isActive       = rc.getIsActive();
        this.totalReferrals = rc.getTotalReferrals();
        this.maxReferrals   = rc.getMaxReferrals();
        this.createdAt      = rc.getCreatedAt();
    }
}