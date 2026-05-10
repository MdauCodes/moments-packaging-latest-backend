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

    public CreditWalletDto(CreditWallet wallet, int creditsPerKes) {
        this.id               = wallet.getId();
        this.balance          = wallet.getBalance();
        this.lifetimeEarned   = wallet.getLifetimeEarned();
        this.lifetimeRedeemed = wallet.getLifetimeRedeemed();
        this.balanceValueKes  = creditsPerKes > 0
                ? BigDecimal.valueOf(wallet.getBalance())
                    .divide(BigDecimal.valueOf(creditsPerKes), 2, RoundingMode.FLOOR)
                : BigDecimal.ZERO;
    }
}