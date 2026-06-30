package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingDto;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.SettingUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SettingsService settingsService;
    private final AuditLogService auditLogService;

    @IsAdmin
    @GetMapping
    public ResponseEntity<List<SettingDto>> getAllSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @IsAdmin
    @PutMapping
    public ResponseEntity<SettingDto> upsertSetting(
            @Valid @RequestBody SettingUpdateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        String oldValue = settingsService.getValue(request.getKey(), null);
        SettingDto updated = settingsService.upsertSetting(request);
        auditLogService.log(actor, "SETTING", request.getKey(), request.getKey(),
                "UPDATE", null,
                "{\"key\":\"" + request.getKey() + "\",\"from\":\"" + oldValue + "\",\"to\":\"" + request.getValue() + "\"}",
                AuditLogService.extractIp(httpRequest));
        return ResponseEntity.ok(updated);
    }
}