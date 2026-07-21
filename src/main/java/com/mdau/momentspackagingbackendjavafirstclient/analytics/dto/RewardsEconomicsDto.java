package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RewardsEconomicsDto(
        Instant rangeStart,
        Instant rangeEnd,
        long outstandingBalanceCoupons,
        BigDecimal outstandingBalanceValueKes,
        long redeemedCouponsInRange,
        BigDecimal redeemedValueKesInRange,
        List<RewardsSourceBreakdownDto> earnedInRange,
        double referralConversionRatePercent,
        long referralSignupsInRange,
        long referralConfirmedInRange,
        BigDecimal estimatedProgramCostKesInRange,
        double medianWalletBalance,
        List<TopWalletHolderDto> topHolders
) {
}
