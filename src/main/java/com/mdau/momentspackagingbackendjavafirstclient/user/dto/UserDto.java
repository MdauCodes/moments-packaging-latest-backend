package com.mdau.momentspackagingbackendjavafirstclient.user.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Permission;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
public class UserDto {

    private final UUID    id;
    private final String  email;
    private final String  firstName;
    private final String  lastName;
    private final Boolean enabled;
    private final Boolean isStaff;
    private final Boolean mustChangePassword;

    // Staff role info
    private final UUID   staffRoleId;
    private final String staffRoleName;
    private final String staffRoleDisplay;

    // Resolved permissions — frontend uses these to gate UI
    private final Set<Permission> permissions;

    private final Instant createdAt;
    private final Instant updatedAt;

    public UserDto(User user) {
        this.id                 = user.getId();
        this.email              = user.getEmail();
        this.firstName          = user.getFirstName();
        this.lastName           = user.getLastName();
        this.enabled            = user.isEnabled();
        this.isStaff            = user.getIsStaff();
        this.mustChangePassword = user.getMustChangePassword();
        this.permissions        = user.getResolvedPermissions();
        this.createdAt          = user.getCreatedAt();
        this.updatedAt          = user.getUpdatedAt();

        if (user.getStaffRole() != null) {
            this.staffRoleId      = user.getStaffRole().getId();
            this.staffRoleName    = user.getStaffRole().getName();
            this.staffRoleDisplay = user.getStaffRole().getDisplayName();
        } else {
            this.staffRoleId      = null;
            this.staffRoleName    = null;
            this.staffRoleDisplay = null;
        }
    }
}