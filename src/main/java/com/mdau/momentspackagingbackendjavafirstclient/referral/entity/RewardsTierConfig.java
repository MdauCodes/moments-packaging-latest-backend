package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * An Individual Shopper's VIP tier ladder (Silver/Gold/Platinum) — deliberately
 * separate from ReferralTierConfig, which sizes a referral *payout* by order
 * value, a different concept from a customer's overall status level.
 */
@Entity
@Table(name = "rewards_tier_configs", indexes = {
        @Index(name = "idx_rewards_tier_min_points", columnList = "min_lifetime_points")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardsTierConfig extends BaseEntity {

    /** Human label — "Silver", "Gold", "Platinum" */
    @Column(name = "tier_name", length = 50, nullable = false)
    private String tierName;

    /** Minimum lifetime points earned to qualify for this tier */
    @Column(name = "min_lifetime_points", nullable = false)
    private Integer minLifetimePoints;

    /** Discount percent applied while a merchant holds this tier */
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "perk_description", length = 255)
    private String perkDescription;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Display order in admin dashboard, and the order tiers are checked (highest first) */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
