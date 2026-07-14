package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

public enum CreditTransactionType {
    EARNED_REFERRAL,    // credited because you referred someone who purchased
    EARNED_PURCHASE,    // credited because you made a purchase via referral
    REDEEMED,           // credits used as discount on an order
    REFUNDED,           // credits returned after order cancellation
    ADJUSTED,           // manual admin adjustment
    EXPIRED,            // credits expired (future feature)
    EARNED_SIGNUP,      // welcome bonus on opening an Individual Shopper Account
    EARNED_REVIEW,      // credited for submitting a product review
    EARNED_ORDER        // general points accrual on any paid order (Individual Shopper, not tied to a referral)
}