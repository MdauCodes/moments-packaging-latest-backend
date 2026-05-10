package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credit_transactions", indexes = {
        @Index(name = "idx_credit_tx_user_id",  columnList = "user_id"),
        @Index(name = "idx_credit_tx_type",     columnList = "type"),
        @Index(name = "idx_credit_tx_ref_event",columnList = "referral_event_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    /** Positive = credit added. Negative = credit deducted */
    @Column(nullable = false)
    private Integer amount;

    /** Balance after this transaction */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(length = 255)
    private String description;

    /** Link back to the referral event that triggered this transaction */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_event_id")
    private ReferralEvent referralEvent;

    /** Link to order if this was a redemption or purchase-based earning */
    @Column(name = "order_id", length = 36)
    private String orderId;
}