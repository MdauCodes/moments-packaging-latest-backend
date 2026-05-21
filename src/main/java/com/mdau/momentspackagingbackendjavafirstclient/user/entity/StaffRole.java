package com.mdau.momentspackagingbackendjavafirstclient.user.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Named role entity — replaces the old Role enum.
 * Predefined roles are seeded on startup. SUPER_ADMIN can create custom roles.
 * Each role carries a set of Permission enum values.
 *
 * Named StaffRole to avoid conflict with Spring Security's Role concept
 * and the existing Role enum (which we keep temporarily for migration).
 */
@Entity
@Table(name = "staff_roles", indexes = {
        @Index(name = "idx_staff_roles_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffRole extends BaseEntity {

    /** Canonical name e.g. SUPER_ADMIN, DISPATCHER, CUSTOM */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Human-readable label shown in admin UI */
    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * true = seeded default role — cannot be deleted.
     * false = custom role created by SUPER_ADMIN.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "staff_role_permissions",
            joinColumns = @JoinColumn(name = "role_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 50)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;
}