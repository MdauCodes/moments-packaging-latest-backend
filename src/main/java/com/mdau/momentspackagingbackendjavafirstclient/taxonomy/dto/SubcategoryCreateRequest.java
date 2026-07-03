package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SubcategoryCreateRequest {

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private Integer sortOrder;
}
