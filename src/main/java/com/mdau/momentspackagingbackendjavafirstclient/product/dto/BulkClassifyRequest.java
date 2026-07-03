package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Bulk admin action: assign a set of products to one Subcategory and/or one
 * set of Industries in a single call. Null subcategoryId/industryIds are
 * left untouched on each product; at least one of the two must be provided.
 */
@Getter
@Setter
public class BulkClassifyRequest {

    @NotEmpty(message = "productIds is required")
    private List<UUID> productIds;

    private UUID subcategoryId;

    private List<UUID> industryIds;
}
