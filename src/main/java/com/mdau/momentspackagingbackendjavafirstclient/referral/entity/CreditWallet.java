package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_wallets", indexes = {
        @Index(name = "idx_credit_wallets_user_id", columnList = "user_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditWallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "balance", nullable = false)
    @Builder.Default
    private Integer balance = 0;

    @Column(name = "lifetime_earned", nullable = false)
    @Builder.Default
    private Integer lifetimeEarned = 0;

    @Column(name = "lifetime_redeemed", nullable = false)
    @Builder.Default
    private Integer lifetimeRedeemed = 0;

    /** How many times this user has redeemed points — gates redemption behind email verification after a free allowance. */
    @Column(name = "redemption_count", nullable = false)
    @Builder.Default
    private Integer redemptionCount = 0;
}