package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RisellerCatalogItem {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Code")
    private String code;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("BaseUomName")
    private String baseUomName;

    /** "Yes" = physical stock tracked in warehouse; "No" = service/virtual item. */
    @JsonProperty("StockItem")
    private String stockItem;

    /** E.g. "SK-634" — prefixed form of the code, alternative search token. */
    @JsonProperty("PrefixedCode")
    private String prefixedCode;

    /** E.g. "FINISHED", "RAW_MATERIAL" */
    @JsonProperty("ItemType")
    private String itemType;

    public boolean isPhysicalStockItem() {
        return "Yes".equalsIgnoreCase(stockItem);
    }
}