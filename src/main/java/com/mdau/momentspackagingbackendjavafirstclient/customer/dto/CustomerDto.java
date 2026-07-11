package com.mdau.momentspackagingbackendjavafirstclient.customer.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Admin-facing customer record — mirrors the frontend's CustomerRecord shape exactly. */
@Getter
public class CustomerDto {

    public enum Segment { RETAIL, WHOLESALE, ENTERPRISE }
    public enum Status { VIP, ACTIVE, AT_RISK, DORMANT }

    private final UUID id;
    private final String name;
    private final String email;
    private final String phone;
    private final String city;
    private final Segment segment;
    private final Status status;
    private final BigDecimal lifetimeValue;
    private final long ordersCount;
    private final Instant lastOrderAt;
    private final Instant firstOrderAt;
    private final BigDecimal averageOrderValue;
    private final String defaultAddress;
    private final Instant createdAt;

    public CustomerDto(UUID id, String name, String email, String phone, String city,
                        Segment segment, Status status, BigDecimal lifetimeValue, long ordersCount,
                        Instant lastOrderAt, Instant firstOrderAt, BigDecimal averageOrderValue,
                        String defaultAddress, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.segment = segment;
        this.status = status;
        this.lifetimeValue = lifetimeValue;
        this.ordersCount = ordersCount;
        this.lastOrderAt = lastOrderAt;
        this.firstOrderAt = firstOrderAt;
        this.averageOrderValue = averageOrderValue;
        this.defaultAddress = defaultAddress;
        this.createdAt = createdAt;
    }
}
