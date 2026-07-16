package com.mdau.momentspackagingbackendjavafirstclient.devtools.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Minimal, cart-independent version of CheckoutRequest — dev-tools testing supplies items directly. */
@Getter
@Setter
public class CheckoutDryRunRequest {

    private String county;
    private String promoCode;
    private Integer redeemPoints;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private String     productId;
        private Integer    quantity;
        private BigDecimal unitPrice;
    }
}
