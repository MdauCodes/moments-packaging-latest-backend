package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_counters", indexes = {
        @Index(name = "idx_order_counters_year_month", columnList = "year_month", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month", nullable = false, unique = true, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    @Builder.Default
    private Integer counter = 0;
}