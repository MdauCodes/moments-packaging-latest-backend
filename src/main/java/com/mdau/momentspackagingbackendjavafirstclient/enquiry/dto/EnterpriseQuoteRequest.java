package com.mdau.momentspackagingbackendjavafirstclient.enquiry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class EnterpriseQuoteRequest {

    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private UUID industryId;

    @NotNull(message = "Estimated quantity is required")
    @Min(value = 10000, message = "Enterprise orders require minimum 10,000 units")
    private Integer estimatedQuantity;

    private String productInterest;

    private String message;
}