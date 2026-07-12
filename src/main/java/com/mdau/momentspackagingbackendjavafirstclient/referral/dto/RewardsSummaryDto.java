package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RewardsSummaryDto {
    /** Sum of all positive credit transaction amounts, across every customer, all-time. */
    private final long totalPointsEarned;
    /** Sum of |amount| for every negative credit transaction (redemptions), all-time. */
    private final long totalPointsRedeemed;
    /** totalPointsEarned - totalPointsRedeemed — points customers could still redeem today. */
    private final long netPointsOutstanding;
    /** totalPointsRedeemed converted to KES — the actual discount cost to the business so far. */
    private final BigDecimal kesValueRedeemed;
    /** netPointsOutstanding converted to KES — potential future discount exposure. */
    private final BigDecimal kesValueOutstanding;
    private final int creditsPerKes;
}
