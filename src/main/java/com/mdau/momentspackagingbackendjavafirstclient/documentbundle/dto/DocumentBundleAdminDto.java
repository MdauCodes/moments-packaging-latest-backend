package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.dto;

import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class DocumentBundleAdminDto {
    private final UUID    id;
    private final String  orderReference;
    private final String  orderStatus;
    private final String  customerName;
    private final String  customerPhone;
    private final String  recipientEmail;
    private final String  status;
    private final String  failureReason;
    private final String  etrCloudinaryUrl;
    private final Instant etrUploadedAt;
    private final Instant sentAt;
    private final Instant createdAt;
    /** Convenience for the admin tab — the receipt/tax-invoice links to preview alongside the ETR before sending. */
    private final String  receiptUrl;
    private final String  taxInvoiceUrl;

    public DocumentBundleAdminDto(DocumentBundle bundle, String receiptUrl, String taxInvoiceUrl) {
        this.id               = bundle.getId();
        this.orderReference   = bundle.getOrder().getReference();
        this.orderStatus      = bundle.getOrder().getStatus().name();
        this.customerName     = bundle.getOrder().getContactName();
        this.customerPhone    = bundle.getOrder().getPhone();
        this.recipientEmail   = bundle.getRecipientEmail();
        this.status           = bundle.getStatus().name();
        this.failureReason    = bundle.getFailureReason();
        this.etrCloudinaryUrl = bundle.getEtrCloudinaryUrl();
        this.etrUploadedAt    = bundle.getEtrUploadedAt();
        this.sentAt           = bundle.getSentAt();
        this.createdAt        = bundle.getCreatedAt();
        this.receiptUrl       = receiptUrl;
        this.taxInvoiceUrl    = taxInvoiceUrl;
    }
}
