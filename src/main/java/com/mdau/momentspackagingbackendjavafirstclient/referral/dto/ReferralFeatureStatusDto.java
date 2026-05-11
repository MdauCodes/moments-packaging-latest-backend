package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReferralFeatureStatusDto {
    /** true = client has paid, feature is available to admin */
    private final boolean featureUnlocked;
    /** true = admin has turned on the referral program */
    private final boolean programEnabled;
    private final int     creditsPerKes;
    private final int     maxRedemptionPercent;
}