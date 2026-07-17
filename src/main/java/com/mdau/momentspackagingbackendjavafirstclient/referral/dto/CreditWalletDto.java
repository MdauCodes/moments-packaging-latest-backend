package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditWallet;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Getter
public class CreditWalletDto {
    private final UUID       id;
    private final Integer    balance;
    private final Integer    lifetimeEarned;
    private final Integer    lifetimeRedeemed;
    /** KES value of current balance based on conversion rate */
    private final BigDecimal balanceValueKes;
    private final Integer    redemptionCount;
    /** Null when the referral feature/program is unavailable or disabled — coupons still work without it. */
    private final String     referralCode;
    private final Boolean    emailVerified;
    /** How many more redemptions are allowed before email verification is required. Null once verified (unlimited). */
    private final Integer    freeRedemptionsRemaining;

    public CreditWalletDto(CreditWallet wallet, int creditsPerKes, String referralCode, boolean emailVerified, int freeRedemptionLimit) {
        this.id               = wallet.getId();
        this.balance           = wallet.getBalance();
        this.lifetimeEarned   = wallet.getLifetimeEarned();
        this.lifetimeRedeemed = wallet.getLifetimeRedeemed();
        this.balanceValueKes  = creditsPerKes > 0
                ? BigDecimal.valueOf(wallet.getBalance())
                    .divide(BigDecimal.valueOf(creditsPerKes), 2, RoundingMode.FLOOR)
                : BigDecimal.ZERO;
        this.redemptionCount     = wallet.getRedemptionCount();
        this.referralCode        = referralCode;
        this.emailVerified       = emailVerified;
        this.freeRedemptionsRemaining = emailVerified
                ? null
                : Math.max(0, freeRedemptionLimit - wallet.getRedemptionCount());
    }
}