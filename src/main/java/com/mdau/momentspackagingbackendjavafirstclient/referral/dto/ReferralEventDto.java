package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralEvent;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralEventStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class ReferralEventDto {
    private final UUID                id;
    private final String              referralCode;
    private final String              refereeEmail;
    private final String              refereeFirstName;
    private final ReferralEventStatus status;
    private final BigDecimal          qualifyingAmount;
    private final Integer             referrerCreditsAwarded;
    private final Integer             refereeCreditsAwarded;
    private final Instant             createdAt;

    public ReferralEventDto(ReferralEvent e) {
        this.id                     = e.getId();
        this.referralCode           = e.getReferralCode();
        this.refereeEmail           = e.getReferee().getEmail();
        this.refereeFirstName       = e.getReferee().getFirstName();
        this.status                 = e.getStatus();
        this.qualifyingAmount       = e.getQualifyingAmount();
        this.referrerCreditsAwarded = e.getReferrerCreditsAwarded();
        this.refereeCreditsAwarded  = e.getRefereeCreditsAwarded();
        this.createdAt              = e.getCreatedAt();
    }
}