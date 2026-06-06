package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RisellerStockItem {

    @JsonProperty("itemId")
    private String itemId;

    @JsonProperty("inStock")
    private BigDecimal inStock;

    @JsonProperty("packUnit")
    private String packUnit;

    @JsonProperty("lastUpdated")
    private String lastUpdated;
}