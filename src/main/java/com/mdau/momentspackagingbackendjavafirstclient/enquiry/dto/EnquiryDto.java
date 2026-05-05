package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class EnquiryDto {

    private final UUID          id;
    private final String        reference;
    private final String        persona;
    private final ContactDto    contact;
    private final String        message;
    private final String        source;
    private final EnquiryStatus status;
    private final String        assignedTo;
    private final String        internalNotes;
    private final List<EnquiryItemDto> items;
    private final Instant       createdAt;
    private final Instant       updatedAt;

    public EnquiryDto(Enquiry enquiry) {
        this.id            = enquiry.getId();
        this.reference     = enquiry.getReference();
        this.persona       = enquiry.getPersona();
        this.message       = enquiry.getMessage();
        this.source        = enquiry.getSource();
        this.status        = enquiry.getStatus();
        this.assignedTo    = enquiry.getAssignedTo();
        this.internalNotes = enquiry.getInternalNotes();
        this.createdAt     = enquiry.getCreatedAt();
        this.updatedAt     = enquiry.getUpdatedAt();
        this.contact       = new ContactDto(
                enquiry.getContactName(),
                enquiry.getEmail(),
                enquiry.getPhone(),
                enquiry.getCompany()
        );
        this.items = enquiry.getItems().stream()
                .map(EnquiryItemDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    public static class ContactDto {
        private final String name;
        private final String email;
        private final String phone;
        private final String company;

        public ContactDto(String name, String email, String phone, String company) {
            this.name    = name;
            this.email   = email;
            this.phone   = phone;
            this.company = company;
        }
    }
}