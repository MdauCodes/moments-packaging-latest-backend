package com.mdau.momentspackagingbackendjavafirstclient.common.exception;

import lombok.Getter;

@Getter
public class PaymentGatewayException extends RuntimeException {

    private final String errorCode;

    public PaymentGatewayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}