package com.mdau.momentspackagingbackendjavafirstclient.industry.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndustryCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String iconUrl;
}