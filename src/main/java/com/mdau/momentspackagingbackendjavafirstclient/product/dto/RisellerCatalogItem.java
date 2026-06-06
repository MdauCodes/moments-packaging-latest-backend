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
}