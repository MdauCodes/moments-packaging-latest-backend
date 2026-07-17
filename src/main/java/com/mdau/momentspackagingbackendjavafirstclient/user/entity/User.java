package com.mdau.momentspackagingbackendjavafirstclient.user.entity;

import com.mdau.momentspackagingbackendjavafirstclient.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email",   columnList = "email"),
        @Index(name = "idx_users_phone",   columnList = "phone"),
        @Index(name = "idx_users_deleted", columnList = "deleted"),
        @Index(name = "idx_users_is_staff", columnList = "is_staff")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String county;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 255)
    private String businessName;

    /**
     * Chosen at registration: INDIVIDUAL_SHOPPER or BUSINESS. Null only for
     * legacy accounts created before this field existed — backfilled by
     * AccountTypeMigrationSeeder on the first startup after this change.
     * Never set for staff/admin accounts.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * True for all staff/admin users created by SUPER_ADMIN.
     * False for customer accounts (registered via storefront).
     * Staff users never appear in customer contexts; customers never
     * appear in admin user management.
     */
    @Column(name = "is_staff", nullable = false)
    @Builder.Default
    private Boolean isStaff = false;

    /**
     * The staff role assigned by SUPER_ADMIN.
     * Null for customer accounts.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_role_id")
    private StaffRole staffRole;

    /**
     * True when user is logging in for the first time with a temp password.
     * Frontend must force a password change before allowing further actions.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    /**
     * Expiry for the temporary password (48 hours after creation).
     * If this instant passes without a first login, the account is auto-deleted
     * by the TempPasswordExpiryJob.
     */
    @Column(name = "temp_password_expires_at")
    private Instant tempPasswordExpiresAt;

    // ── Legacy roles (kept for customer auth compatibility) ───────────────────
    // Staff permissions are now derived from staffRole.permissions.
    // Customer role is still stored here for the customer auth flow.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // ── Spring Security ───────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Legacy role-based authorities (used for customer auth + backward compat)
        roles.forEach(role ->
                authorities.add(new SimpleGrantedAuthority(role.name())));

        // Permission-based authorities from staffRole
        if (staffRole != null && staffRole.getPermissions() != null) {
            staffRole.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority("PERM_" + permission.name())));
        }

        // Derive ROLE_ADMIN straight from staffRole rather than relying on the legacy `roles`
        // set staying in sync — that set is only written at account create/update time, so any
        // ADMIN-tier staff account created or edited outside that path could otherwise end up
        // missing delete authority despite having the right staffRole.
        if (staffRole != null && "SUPER_ADMIN".equals(staffRole.getName())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (staffRole != null && "ADMIN".equals(staffRole.getName())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return authorities;
    }

    /** Resolved permissions from staffRole — convenience method for JWT generation */
    public Set<Permission> getResolvedPermissions() {
        if (staffRole == null || staffRole.getPermissions() == null) return Set.of();
        return staffRole.getPermissions();
    }

    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled && !Boolean.TRUE.equals(deleted); }

    public String getFullName() { return firstName + " " + lastName; }
}