package com.mdau.momentspackagingbackendjavafirstclient.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BusinessAccountCreateRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "KRA PIN is required")
    private String kraPin;

    @NotBlank(message = "Business registration number is required")
    private String businessRegNumber;

    private UUID industryId;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    private String contactPersonRole;

    @NotBlank(message = "Phone is required")
    private String phone;
}
