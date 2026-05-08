package com.mdau.momentspackagingbackendjavafirstclient.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    /**
     * The collection tier to purchase. NULL means individual unit purchase.
     * Will be rejected if product.individualSalesEnabled = false.
     */
    private UUID tierId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private String size;
    private String material;
    private String finish;
    private String sessionId;
}