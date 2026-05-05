package com.mdau.momentspackagingbackendjavafirstclient.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CartDto {
    private UUID            id;
    private String          sessionId;
    private List<CartItemDto> items;
    private int             itemCount;
    private BigDecimal      subtotal;
    private Instant         createdAt;
}