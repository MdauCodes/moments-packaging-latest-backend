package com.mdau.momentspackagingbackendjavafirstclient.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MockModeStatusDto {
    private final boolean enabled;
    private final String  message;
}