package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import com.mdau.momentspackagingbackendjavafirstclient.settings.entity.AppSetting;
import lombok.Getter;

@Getter
public class SettingDto {

    private final Long   id;
    private final String key;
    private final String value;
    private final String description;

    public SettingDto(AppSetting setting) {
        this.id          = setting.getId();
        this.key         = setting.getKey();
        this.value       = setting.getValue();
        this.description = setting.getDescription();
    }
}