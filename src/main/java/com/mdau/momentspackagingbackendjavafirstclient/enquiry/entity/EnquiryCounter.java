package com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "enquiry_counters", indexes = {
        @Index(name = "idx_enquiry_counters_year_month",
               columnList = "year_month", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnquiryCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month", nullable = false, unique = true, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    @Builder.Default
    private Integer counter = 0;
}