package com.mdau.momentspackagingbackendjavafirstclient.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayHeroCallbackDto {

    @JsonProperty("response")
    private PayHeroCallbackResponse response;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PayHeroCallbackResponse {

        @JsonProperty("MerchantRequestID")
        private String merchantRequestId;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestId;

        @JsonProperty("ResultCode")
        private Integer resultCode;

        @JsonProperty("ResultDesc")
        private String resultDesc;

        @JsonProperty("MpesaReceiptNumber")
        private String mpesaReceiptNumber;

        @JsonProperty("Amount")
        private Double amount;

        @JsonProperty("PhoneNumber")
        private String phoneNumber;
    }
}