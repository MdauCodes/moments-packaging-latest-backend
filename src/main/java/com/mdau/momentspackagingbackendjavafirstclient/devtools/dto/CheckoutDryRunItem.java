package com.mdau.momentspackagingbackendjavafirstclient.devtools.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CheckoutDryRunItem {
    private String     productId;
    private String     productName;
    private Integer     quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private boolean     found;
}
