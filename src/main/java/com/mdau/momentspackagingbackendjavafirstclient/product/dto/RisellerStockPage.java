package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RisellerStockPage {

    @JsonProperty("hasNextPage")
    private boolean hasNextPage;

    @JsonProperty("currentPage")
    private int currentPage;

    @JsonProperty("pageCount")
    private int pageCount;

    @JsonProperty("data")
    private List<RisellerStockItem> data;
}