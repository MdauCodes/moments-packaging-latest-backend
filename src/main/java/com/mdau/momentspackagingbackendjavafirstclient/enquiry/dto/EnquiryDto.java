package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class EnquiryDto {

    private final UUID          id;
    private final String        persona;
    private final String        contactName;
    private final String        email;
    private final String        phone;
    private final String        company;
    private final String        message;
    private final String        source;
    private final EnquiryStatus status;
    private final UUID          assignedToId;
    private final String        assignedToName;
    private final String        internalNotes;
    private final Instant       followUpAt;
    private final Instant       firstContactedAt;
    private final BigDecimal    estimatedValue;
    private final String        productInterest;
    private final Instant       createdAt;
    private final Instant       updatedAt;

    public EnquiryDto(Enquiry e) {
        this.id               = e.getId();
        this.persona          = e.getPersona();
        this.contactName      = e.getContactName();
        this.email            = e.getEmail();
        this.phone            = e.getPhone();
        this.company          = e.getCompany();
        this.message          = e.getMessage();
        this.source           = e.getSource();
        this.status           = e.getStatus();
        this.assignedToId     = e.getAssignedToId();
        this.assignedToName   = e.getAssignedToName();
        this.internalNotes    = e.getInternalNotes();
        this.followUpAt       = e.getFollowUpAt();
        this.firstContactedAt = e.getFirstContactedAt();
        this.estimatedValue   = e.getEstimatedValue();
        this.productInterest  = e.getProductInterest();
        this.createdAt        = e.getCreatedAt();
        this.updatedAt        = e.getUpdatedAt();
    }
}