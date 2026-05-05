package com.mdau.momentspackagingbackendjavafirstclient.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String deliveryAddress;
    private String city;
    private String county;
    private String postalCode;
    private String businessName;
}