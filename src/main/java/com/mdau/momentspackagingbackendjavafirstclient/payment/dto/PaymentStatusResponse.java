package com.mdau.momentspackagingbackendjavafirstclient.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Unified payment status response.
 * Status is always one of: PROCESSING | SUCCESS | FAILED | NO_PAYMENT
 * This contract is what the frontend polls against.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatusResponse {

    private UUID       orderId;
    private String     orderReference;
    private String     status;          // PROCESSING | SUCCESS | FAILED | NO_PAYMENT
    private String     message;
    private BigDecimal amount;
    private String     receiptNumber;
    private String     failureReason;
    private String     paymentMethod;
}