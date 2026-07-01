package com.mdau.momentspackagingbackendjavafirstclient.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Daraja STK Push callback payload shape.
 * POST arrives at /api/v1/payments/daraja/callback/{secret} — see
 * DarajaService.buildCallbackUrl() for how the secret is derived and
 * embedded in the CallBackURL sent with each STK push request.
 *
 * Success result example:
 * {
 *   "Body": {
 *     "stkCallback": {
 *       "MerchantRequestID": "...",
 *       "CheckoutRequestID": "ws_CO_...",
 *       "ResultCode": 0,
 *       "ResultDesc": "The service request is processed successfully.",
 *       "CallbackMetadata": {
 *         "Item": [
 *           { "Name": "Amount",            "Value": 1000 },
 *           { "Name": "MpesaReceiptNumber","Value": "QKM7XXXXX" },
 *           { "Name": "PhoneNumber",        "Value": 254712345678 }
 *         ]
 *       }
 *     }
 *   }
 * }
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DarajaCallbackDto {

    @JsonProperty("Body")
    private Body body;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StkCallback {
        @JsonProperty("MerchantRequestID")
        private String merchantRequestId;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestId;

        @JsonProperty("ResultCode")
        private Integer resultCode;

        @JsonProperty("ResultDesc")
        private String resultDesc;

        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallbackMetadata {
        @JsonProperty("Item")
        private List<MetadataItem> item;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataItem {
        @JsonProperty("Name")
        private String name;

        @JsonProperty("Value")
        private Object value;
    }

    /** Extracts a metadata item value by name. Returns null if not found. */
    public String getMetadataValue(String name) {
        if (body == null || body.getStkCallback() == null) return null;
        StkCallback cb = body.getStkCallback();
        if (cb.getCallbackMetadata() == null || cb.getCallbackMetadata().getItem() == null) return null;
        return cb.getCallbackMetadata().getItem().stream()
                .filter(i -> name.equalsIgnoreCase(i.getName()))
                .findFirst()
                .map(i -> i.getValue() != null ? String.valueOf(i.getValue()) : null)
                .orElse(null);
    }
}