package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockAdjustmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustRequest {

    @NotNull(message = "Adjustment type is required")
    private StockAdjustmentType type;

    /** Positive = add stock. Negative = remove stock. */
    @NotNull(message = "Delta is required")
    private Integer delta;

    private String reason;
}