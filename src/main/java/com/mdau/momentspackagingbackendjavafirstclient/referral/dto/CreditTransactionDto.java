package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransaction;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransactionType;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CreditTransactionDto {
    private final UUID                  id;
    private final CreditTransactionType type;
    private final Integer               amount;
    private final Integer               balanceAfter;
    private final String                description;
    private final String                orderId;
    private final Instant               createdAt;

    public CreditTransactionDto(CreditTransaction tx) {
        this.id          = tx.getId();
        this.type        = tx.getType();
        this.amount      = tx.getAmount();
        this.balanceAfter= tx.getBalanceAfter();
        this.description = tx.getDescription();
        this.orderId     = tx.getOrderId();
        this.createdAt   = tx.getCreatedAt();
    }
}