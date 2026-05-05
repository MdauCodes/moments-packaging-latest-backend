package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingUpdateRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @NotNull(message = "Value is required")
    private String value;

    private String description;
}