package com.mdau.momentspackagingbackendjavafirstclient.user.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.StaffRoleDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Permission;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.StaffRoleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService         userService;
    private final StaffRoleRepository roleRepository;
    private final AuditLogService     auditLogService;

    // ── User management ───────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('PERM_USER_VIEW') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserDto>> getAllStaffUsers(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PageResponse<>(userService.getAllStaffUsers(pageable)));
    }

    @PreAuthorize("hasAuthority('PERM_USER_VIEW') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PreAuthorize("hasAuthority('PERM_USER_CREATE') or hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        UserDto created = userService.createStaffUser(request);
        auditLogService.log(actor, "USER", created.getId().toString(),
                created.getEmail(), "CREATE", null,
                "{\"email\":\"" + created.getEmail() + "\",\"role\":\"" + created.getStaffRoleName() + "\"}",
                httpRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasAuthority('PERM_USER_MANAGE_ROLES') or hasRole('ROLE_SUPER_ADMIN')")
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        UserDto updated = userService.updateStaffUser(id, request);
        String changes = Boolean.TRUE.equals(request.getResetPassword())
                ? "{\"action\":\"password_reset\"}"
                : "{\"enabled\":" + request.getEnabled() + "}";
        auditLogService.log(actor, "USER", id.toString(), updated.getEmail(),
                Boolean.TRUE.equals(request.getResetPassword()) ? "PASSWORD_RESET" : "UPDATE",
                null, changes, httpRequest);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        UserDto target = userService.getById(id);
        userService.deleteStaffUser(id);
        auditLogService.log(actor, "USER", id.toString(), target.getEmail(),
                "DELETE", null, null, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('PERM_ORDER_ASSIGN') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users/assignable")
    public ResponseEntity<List<UserDto>> getAssignableStaff() {
        return ResponseEntity.ok(
                userService.getAllStaffUsers(Pageable.unpaged())
                        .stream().collect(Collectors.toList()));
    }

    // ── Role management ───────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/roles")
    public ResponseEntity<List<StaffRoleDto>> getAllRoles() {
        return ResponseEntity.ok(
                roleRepository.findByDeletedFalseOrderByIsDefaultDescNameAsc()
                        .stream().map(StaffRoleDto::new).collect(Collectors.toList()));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/roles")
    public ResponseEntity<StaffRoleDto> createRole(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        String name = ((String) body.get("name")).toUpperCase().trim().replace(" ", "_");
        String displayName = (String) body.get("displayName");
        String description = (String) body.getOrDefault("description", "");
        @SuppressWarnings("unchecked")
        List<String> permList = (List<String>) body.getOrDefault("permissions", List.of());
        Set<Permission> permissions = parsePermissions(permList);
        if (roleRepository.existsByNameAndDeletedFalse(name)) {
            throw new IllegalArgumentException("Role '" + name + "' already exists.");
        }
        StaffRole role = roleRepository.save(StaffRole.builder()
                .name(name).displayName(displayName).description(description)
                .isDefault(false).permissions(permissions).deleted(false).build());
        auditLogService.log(actor, "ROLE", role.getId().toString(), name,
                "CREATE", null, "{\"name\":\"" + name + "\"}", httpRequest);
        URI location = URI.create("/api/v1/admin/roles/" + role.getId());
        return ResponseEntity.created(location).body(new StaffRoleDto(role));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PatchMapping("/roles/{id}")
    public ResponseEntity<StaffRoleDto> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        StaffRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (body.containsKey("displayName")) role.setDisplayName((String) body.get("displayName"));
        if (body.containsKey("description")) role.setDescription((String) body.get("description"));
        if (body.containsKey("permissions")) {
            @SuppressWarnings("unchecked")
            List<String> permList = (List<String>) body.get("permissions");
            role.setPermissions(parsePermissions(permList));
        }
        StaffRole saved = roleRepository.save(role);
        auditLogService.log(actor, "ROLE", id.toString(), role.getName(),
                "UPDATE", null, null, httpRequest);
        return ResponseEntity.ok(new StaffRoleDto(saved));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        StaffRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (Boolean.TRUE.equals(role.getIsDefault())) {
            throw new IllegalArgumentException("Default roles cannot be deleted.");
        }
        role.setDeleted(true);
        roleRepository.save(role);
        auditLogService.log(actor, "ROLE", id.toString(), role.getName(),
                "DELETE", null, null, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> getAllPermissions() {
        return ResponseEntity.ok(
                Arrays.stream(Permission.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
    }

    private Set<Permission> parsePermissions(List<String> raw) {
        return raw.stream()
                .map(p -> {
                    String normalised = p.startsWith("PERM_") ? p.substring(5) : p;
                    try {
                        return Permission.valueOf(normalised);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Unknown permission: '" + p + "'. Valid values: "
                                        + Arrays.stream(Permission.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")));
                    }
                })
                .collect(Collectors.toSet());
    }
}