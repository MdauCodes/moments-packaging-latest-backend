package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Backs the internal, sequential invoice numbering used once an order's
 * payment is confirmed (see InvoiceNumberGenerator). Scoped per calendar
 * year, not per month like OrderCounter — matches typical Kenyan sequential
 * invoice-numbering expectation and eases year-end reconciliation.
 */
@Entity
@Table(name = "invoice_counters", indexes = {
        @Index(name = "idx_invoice_counters_year", columnList = "year", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 4)
    private String year;

    @Column(nullable = false)
    @Builder.Default
    private Integer counter = 0;
}
