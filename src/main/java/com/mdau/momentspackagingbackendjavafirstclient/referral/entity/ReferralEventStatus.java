package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

public enum ReferralEventStatus {
    /** Referee signed up but hasn't made a qualifying purchase yet */
    PENDING,
    /** Qualifying purchase made — credits awarded */
    CONFIRMED,
    /** Referee never purchased within allowed window */
    EXPIRED,
    /** Manually voided by admin */
    VOIDED
}