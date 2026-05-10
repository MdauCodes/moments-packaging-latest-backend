package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "referral_events", indexes = {
        @Index(name = "idx_ref_events_referrer",  columnList = "referrer_id"),
        @Index(name = "idx_ref_events_referee",   columnList = "referee_id"),
        @Index(name = "idx_ref_events_order",     columnList = "order_id"),
        @Index(name = "idx_ref_events_status",    columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralEvent extends BaseEntity {

    /** The user who shared their referral code */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    /** The user who signed up using the referral code */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    private User referee;

    /** The referral code used */
    @Column(name = "referral_code", length = 20, nullable = false)
    private String referralCode;

    /** The qualifying order that triggered credit award. Null until purchase made. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReferralEventStatus status = ReferralEventStatus.PENDING;

    /** Order amount that triggered the event */
    @Column(name = "qualifying_amount", precision = 12, scale = 2)
    private BigDecimal qualifyingAmount;

    /** Credits awarded to referrer */
    @Column(name = "referrer_credits_awarded", nullable = false)
    @Builder.Default
    private Integer referrerCreditsAwarded = 0;

    /** Credits awarded to referee */
    @Column(name = "referee_credits_awarded", nullable = false)
    @Builder.Default
    private Integer refereeCreditsAwarded = 0;
}