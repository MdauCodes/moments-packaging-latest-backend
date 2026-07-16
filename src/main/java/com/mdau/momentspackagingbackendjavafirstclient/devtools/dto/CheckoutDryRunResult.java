package com.mdau.momentspackagingbackendjavafirstclient.devtools.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CheckoutDryRunResult {
    private List<CheckoutDryRunItem> items;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private BigDecimal taxableAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private String      appliedPromo;
    private List<String> warnings;
}
