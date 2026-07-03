package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class BulkClassifyResponse {

    private final int updatedCount;
    private final List<UUID> productIds;

    public BulkClassifyResponse(int updatedCount, List<UUID> productIds) {
        this.updatedCount = updatedCount;
        this.productIds = productIds;
    }
}
