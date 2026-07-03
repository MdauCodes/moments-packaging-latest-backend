package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SegmentCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private Integer sortOrder;
}
