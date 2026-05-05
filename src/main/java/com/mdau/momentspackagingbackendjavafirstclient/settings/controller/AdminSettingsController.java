package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingDto;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingsService settingsService;

    @IsAdmin
    @GetMapping
    public ResponseEntity<List<SettingDto>> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @IsAdmin
    @PutMapping
    public ResponseEntity<SettingDto> upsertSetting(
            @Valid @RequestBody SettingUpdateRequest request) {
        return ResponseEntity.ok(settingsService.upsertSetting(request));
    }
}