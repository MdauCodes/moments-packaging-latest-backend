package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingUpdateRequest {

    @NotBlank(message = "Setting key is required")
    private String key;

    private String value;

    private String description;
}