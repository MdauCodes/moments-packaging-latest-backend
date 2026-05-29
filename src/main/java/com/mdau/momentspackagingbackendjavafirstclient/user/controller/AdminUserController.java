package com.mdau.momentspackagingbackendjavafirstclient.user.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.StaffRoleDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Permission;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.StaffRoleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.service.UserService;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ── User management ───────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('PERM_USER_VIEW') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserDto>> getAllStaffUsers(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PageResponse<>(
                userService.getAllStaffUsers(pageable)));
    }

    @PreAuthorize("hasAuthority('PERM_USER_VIEW') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PreAuthorize("hasAuthority('PERM_USER_CREATE') or hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<UserDto> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        UserDto created = userService.createStaffUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasAuthority('PERM_USER_MANAGE_ROLES') or hasRole('ROLE_SUPER_ADMIN')")
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateStaffUser(id, request));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteStaffUser(id);
        return ResponseEntity.noContent().build();
    }

    /** Returns all active staff for order assignment dropdown — no customer accounts */
    @PreAuthorize("hasAuthority('PERM_ORDER_ASSIGN') or hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/users/assignable")
    public ResponseEntity<List<UserDto>> getAssignableStaff() {
        return ResponseEntity.ok(
                userService.getAllStaffUsers(Pageable.unpaged())
                        .stream().collect(Collectors.toList()));
    }

    // ── Role management (SUPER_ADMIN only) ────────────────────────────────────

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/roles")
    public ResponseEntity<List<StaffRoleDto>> getAllRoles() {
        return ResponseEntity.ok(
                roleRepository.findByDeletedFalseOrderByIsDefaultDescNameAsc()
                        .stream().map(StaffRoleDto::new).collect(Collectors.toList()));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PostMapping("/roles")
    public ResponseEntity<StaffRoleDto> createRole(@RequestBody Map<String, Object> body) {
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
                .isDefault(false).permissions(permissions).deleted(false)
                .build());

        URI location = URI.create("/api/v1/admin/roles/" + role.getId());
        return ResponseEntity.created(location).body(new StaffRoleDto(role));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @PatchMapping("/roles/{id}")
    public ResponseEntity<StaffRoleDto> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        StaffRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));

        if (body.containsKey("displayName"))
            role.setDisplayName((String) body.get("displayName"));
        if (body.containsKey("description"))
            role.setDescription((String) body.get("description"));
        if (body.containsKey("permissions")) {
            @SuppressWarnings("unchecked")
            List<String> permList = (List<String>) body.get("permissions");
            role.setPermissions(parsePermissions(permList));
        }

        return ResponseEntity.ok(new StaffRoleDto(roleRepository.save(role)));
    }

    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        StaffRole role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        if (Boolean.TRUE.equals(role.getIsDefault())) {
            throw new IllegalArgumentException("Default roles cannot be deleted.");
        }
        role.setDeleted(true);
        roleRepository.save(role);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns all available permissions — frontend uses this to build the role-editor UI.
     * Values are returned WITHOUT the "PERM_" prefix — they match the Permission enum names
     * exactly and must be sent back to POST/PATCH /roles in the same format.
     */
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> getAllPermissions() {
        return ResponseEntity.ok(
                Arrays.stream(Permission.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts a list of permission strings from the frontend into Permission enum values.
     * Tolerates both formats the frontend might send:
     *   - Prefixed:   "PERM_ORDER_VIEW"  → strips prefix → ORDER_VIEW
     *   - Unprefixed: "ORDER_VIEW"        → used as-is
     *
     * Throws IllegalArgumentException (→ 400) on unrecognised values instead of
     * letting valueOf() bubble up as an unhandled 500.
     */
    private Set<Permission> parsePermissions(List<String> raw) {
        return raw.stream()
                .map(p -> {
                    // Normalise: strip the "PERM_" prefix if present
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