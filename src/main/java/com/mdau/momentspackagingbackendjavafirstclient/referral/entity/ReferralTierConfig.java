package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "referral_tier_configs", indexes = {
        @Index(name = "idx_ref_tier_min_amount", columnList = "min_order_amount")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralTierConfig extends BaseEntity {

    /** Minimum order amount (KES) to qualify for this tier */
    @Column(name = "min_order_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal minOrderAmount;

    /** Maximum order amount (KES) for this tier. null = no upper limit */
    @Column(name = "max_order_amount", precision = 12, scale = 2)
    private BigDecimal maxOrderAmount;

    /** Credits awarded to the referrer when this tier is triggered */
    @Column(name = "referrer_credits", nullable = false)
    private Integer referrerCredits;

    /** Credits awarded to the referee (the one who purchased) */
    @Column(name = "referee_credits", nullable = false)
    private Integer refereeCredits;

    /** Human label for admin dashboard e.g. "Bronze", "Silver", "Gold" */
    @Column(name = "tier_name", length = 50, nullable = false)
    private String tierName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Display order in admin dashboard */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}