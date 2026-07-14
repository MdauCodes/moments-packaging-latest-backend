package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Single snapshot the frontend fetches once to drive the margin-aware referral
 * tier calculator entirely client-side — no further backend calls until the
 * admin explicitly seeds tiers.
 */
@Getter
@AllArgsConstructor
public class MarginSummaryDto {
    /** Blended average gross-profit % across all active products with real cost data. Null if none synced yet. */
    private BigDecimal blendedGrossProfitPercent;

    private long productsWithCostData;
    private long totalActiveProducts;

    /** Current credits-per-KES conversion rate, e.g. 10 = 10 points per KES 1. */
    private int creditsPerKes;

    /** Existing tiers, if any — used to pre-fill Manual Mode / warn before Auto Mode overwrites them. */
    private List<ReferralTierConfigDto> existingTiers;
}
