package com.mdau.momentspackagingbackendjavafirstclient.settings.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsSuperAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.settings.dto.MockModeStatusDto;
import com.mdau.momentspackagingbackendjavafirstclient.settings.service.MockModeService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/mock-mode")
@RequiredArgsConstructor
public class AdminMockModeController {

    private final MockModeService mockModeService;
    private final AuditLogService auditLogService;

    /** GET current mock mode status. SUPER_ADMIN only. */
    @IsSuperAdmin
    @GetMapping
    public ResponseEntity<MockModeStatusDto> getStatus() {
        boolean enabled = mockModeService.isMockModeEnabled();
        return ResponseEntity.ok(new MockModeStatusDto(enabled,
                enabled
                ? "Mock mode ON — test data flagged, emails/payments suppressed"
                : "Mock mode OFF — live customer mode"));
    }

    /** PUT /api/v1/admin/mock-mode?enabled=true|false */
    @IsSuperAdmin
    @PutMapping
    public ResponseEntity<MockModeStatusDto> setMockMode(
            @RequestParam boolean enabled,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {

        mockModeService.setMockMode(enabled, actor.getEmail());

        auditLogService.log(actor, "SETTING", "mock.mode.enabled", "Mock Mode",
                enabled ? "MOCK_MODE_ON" : "MOCK_MODE_OFF",
                "SUPER_ADMIN toggled mock mode",
                "{\"enabled\":" + enabled + "}",
                httpRequest);

        return ResponseEntity.ok(new MockModeStatusDto(enabled,
                enabled
                ? "Mock mode ENABLED — all new data flagged as test"
                : "Mock mode DISABLED — live mode restored"));
    }
}