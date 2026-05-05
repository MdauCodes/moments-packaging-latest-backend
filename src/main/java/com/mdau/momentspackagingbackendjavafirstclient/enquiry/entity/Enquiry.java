package com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enquiries", indexes = {
        @Index(name = "idx_enquiries_reference",  columnList = "reference",   unique = true),
        @Index(name = "idx_enquiries_status",     columnList = "status"),
        @Index(name = "idx_enquiries_email",      columnList = "email"),
        @Index(name = "idx_enquiries_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Column(length = 50)
    private String persona;

    @Column(nullable = false, length = 255)
    private String contactName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EnquiryStatus status = EnquiryStatus.NEW;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EnquiryItem> items = new ArrayList<>();
}