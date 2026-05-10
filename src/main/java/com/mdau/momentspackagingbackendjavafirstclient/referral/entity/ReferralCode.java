package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "referral_codes", indexes = {
        @Index(name = "idx_referral_codes_code",    columnList = "code",    unique = true),
        @Index(name = "idx_referral_codes_user_id", columnList = "user_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralCode extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Short alphanumeric code e.g. "JANET2024" */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** How many successful referrals this code has triggered */
    @Column(name = "total_referrals", nullable = false)
    @Builder.Default
    private Integer totalReferrals = 0;

    /** Cap: max referrals allowed. null = unlimited (respects global config) */
    @Column(name = "max_referrals")
    private Integer maxReferrals;
}