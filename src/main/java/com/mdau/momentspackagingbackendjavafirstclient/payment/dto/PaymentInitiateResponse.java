package com.mdau.momentspackagingbackendjavafirstclient.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PaymentInitiateResponse {
    private UUID   orderId;
    private String reference;
    private String status;
    private String message;
    private BigDecimal amount;
    private String receiptNumber;
}