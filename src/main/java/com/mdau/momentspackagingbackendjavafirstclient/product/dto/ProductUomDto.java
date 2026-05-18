package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductUomDto {

    private UUID    id;
    private String  code;
    private String  name;
    private String  description;
    private Boolean isDefault;
    private Integer sortOrder;

    public ProductUomDto(ProductUom uom) {
        this.id          = uom.getId();
        this.code        = uom.getCode();
        this.name        = uom.getName();
        this.description = uom.getDescription();
        this.isDefault   = uom.getIsDefault();
        this.sortOrder   = uom.getSortOrder();
    }
}