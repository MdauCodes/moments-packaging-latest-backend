package com.mdau.momentspackagingbackendjavafirstclient.referral.entity;

public enum CreditTransactionType {
    EARNED_REFERRAL,    // credited because you referred someone who purchased
    EARNED_PURCHASE,    // credited because you made a purchase via referral
    REDEEMED,           // credits used as discount on an order
    REFUNDED,           // credits returned after order cancellation
    ADJUSTED,           // manual admin adjustment
    EXPIRED             // credits expired (future feature)
}