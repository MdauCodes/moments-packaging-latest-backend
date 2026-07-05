package com.mdau.momentspackagingbackendjavafirstclient.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
