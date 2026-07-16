package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto;

import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/** Customer-facing view of a tax document — no internal fields like failureReason. */
@Getter
public class TaxDocumentCustomerDto {
    private final UUID    id;
    private final String  orderReference;
    private final String  status;
    private final String  cloudinaryUrl;
    private final Instant sentAt;
    private final Instant createdAt;

    public TaxDocumentCustomerDto(TaxDocument doc) {
        this.id             = doc.getId();
        this.orderReference = doc.getOrder().getReference();
        this.status         = doc.getStatus().name();
        this.cloudinaryUrl  = doc.getCloudinaryUrl();
        this.sentAt         = doc.getSentAt();
        this.createdAt      = doc.getCreatedAt();
    }
}
