package com.mdau.momentspackagingbackendjavafirstclient.payment.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PaymentInitiateRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String phone;
}