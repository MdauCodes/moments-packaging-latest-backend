package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CategoryCreateRequest {

    @NotNull(message = "Segment is required")
    private UUID segmentId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private Integer sortOrder;
}
