package com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "enquiry_items", indexes = {
        @Index(name = "idx_enquiry_items_enquiry_id", columnList = "enquiry_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id", nullable = false)
    private Enquiry enquiry;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(length = 100)
    private String size;

    @Column(length = 100)
    private String material;

    @Column(length = 100)
    private String finish;

    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String notes;
}