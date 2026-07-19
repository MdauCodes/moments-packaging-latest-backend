package com.mdau.momentspackagingbackendjavafirstclient.receipt.dto;

import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.Receipt;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReceiptAdminDto {
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

    public ReceiptAdminDto(Receipt receipt) {
        this.id             = receipt.getId();
        this.orderReference = receipt.getOrder().getReference();
        this.customerName   = receipt.getOrder().getContactName();
        this.customerPhone  = receipt.getOrder().getPhone();
        this.recipientEmail = receipt.getRecipientEmail();
        this.status         = receipt.getStatus().name();
        this.failureReason  = receipt.getFailureReason();
        this.cloudinaryUrl  = receipt.getCloudinaryUrl();
        this.sentAt         = receipt.getSentAt();
        this.createdAt      = receipt.getCreatedAt();
    }
}
