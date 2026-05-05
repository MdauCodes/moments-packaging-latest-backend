package com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enquiries", indexes = {
        @Index(name = "idx_enquiries_email",      columnList = "email"),
        @Index(name = "idx_enquiries_source",     columnList = "source"),
        @Index(name = "idx_enquiries_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enquiry extends BaseEntity {

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
}