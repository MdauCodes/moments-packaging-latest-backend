package com.mdau.momentspackagingbackendjavafirstclient.business.dto;

import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BusinessAccountCreateRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    private BusinessType businessType;

    /** Optional here — only required later at trade-credit application. */
    private String kraPin;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Road is required")
    private String road;

    @NotBlank(message = "Building address is required")
    private String buildingAddress;

    private UUID industryId;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    private String contactPersonRole;

    @NotBlank(message = "Phone is required")
    private String phone;
}
