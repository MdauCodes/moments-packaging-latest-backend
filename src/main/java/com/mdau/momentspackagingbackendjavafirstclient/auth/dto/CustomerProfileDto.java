package com.mdau.momentspackagingbackendjavafirstclient.auth.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CustomerProfileDto {
    private final UUID    id;
    private final String  email;
    private final String  firstName;
    private final String  lastName;
    private final String  phone;
    private final Boolean emailVerified;
    private final String  deliveryAddress;
    private final String  city;
    private final String  county;
    private final String  postalCode;
    private final String  businessName;
    private final Instant createdAt;
    private final Instant updatedAt;

    public CustomerProfileDto(User user) {
        this.id              = user.getId();
        this.email           = user.getEmail();
        this.firstName       = user.getFirstName();
        this.lastName        = user.getLastName();
        this.phone           = user.getPhone();
        this.emailVerified   = user.getEmailVerified();
        this.deliveryAddress = user.getDeliveryAddress();
        this.city            = user.getCity();
        this.county          = user.getCounty();
        this.postalCode      = user.getPostalCode();
        this.businessName    = user.getBusinessName();
        this.createdAt       = user.getCreatedAt();
        this.updatedAt       = user.getUpdatedAt();
    }
}