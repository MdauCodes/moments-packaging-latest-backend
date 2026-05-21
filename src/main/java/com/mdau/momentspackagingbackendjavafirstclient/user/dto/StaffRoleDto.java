package com.mdau.momentspackagingbackendjavafirstclient.user.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Permission;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StaffRoleDto {

    private UUID            id;
    private String          name;
    private String          displayName;
    private String          description;
    private Boolean         isDefault;
    private Set<Permission> permissions;

    public StaffRoleDto(StaffRole role) {
        this.id          = role.getId();
        this.name        = role.getName();
        this.displayName = role.getDisplayName();
        this.description = role.getDescription();
        this.isDefault   = role.getIsDefault();
        this.permissions = role.getPermissions();
    }
}