package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.dto;

import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class TaxDocumentAdminDto {
    private final UUID    id;
    private final String  orderReference;
    private final String  customerName;
    private final String  customerPhone;
    private final String  recipientEmail;
    private final String  status;
    private final String  failureReason;
    private final String  cloudinaryUrl;
    private final Instant sentAt;
    private final Instant createdAt;

    public TaxDocumentAdminDto(TaxDocument doc) {
        this.id             = doc.getId();
        this.orderReference = doc.getOrder().getReference();
        this.customerName   = doc.getOrder().getContactName();
        this.customerPhone  = doc.getOrder().getPhone();
        this.recipientEmail = doc.getRecipientEmail();
        this.status         = doc.getStatus().name();
        this.failureReason  = doc.getFailureReason();
        this.cloudinaryUrl  = doc.getCloudinaryUrl();
        this.sentAt         = doc.getSentAt();
        this.createdAt      = doc.getCreatedAt();
    }
}
