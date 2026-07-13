package com.mdau.momentspackagingbackendjavafirstclient.user.service;

import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.StaffRoleRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository      userRepository;
    private final StaffRoleRepository roleRepository;
    private final PasswordEncoder     passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService        emailService;

    @Transactional(readOnly = true)
    public List<UserDto> getAllStaffUsers() {
        return userRepository.findByIsStaffTrueAndDeletedFalse(Pageable.unpaged())
                .stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllStaffUsers(Pageable pageable) {
        return userRepository.findByIsStaffTrueAndDeletedFalse(pageable).map(UserDto::new);
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userRepository.findById(id)
                .map(UserDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * Creates a staff user account.
     * Generates a temp password in format MP-{localpart}{3-digit-random}.
     * Sends invite email with temp password.
     * Account auto-deletes after 48 hours if not activated.
     */
    @Transactional
    public UserDto createStaffUser(UserCreateRequest request) {
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        StaffRole role = roleRepository.findById(request.getStaffRoleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role not found: " + request.getStaffRoleId()));

        String tempPassword = generateTempPassword(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(tempPassword))
                .enabled(true)
                .isStaff(true)
                .staffRole(role)
                .mustChangePassword(true)
                .tempPasswordExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .emailVerified(false)
                .deleted(false)
                .roles(rolesForStaffRole(role))
                .build();

        User saved = userRepository.save(user);
        emailService.sendStaffInviteEmail(saved, tempPassword);
        log.info("Staff user created: {} with role: {}", saved.getEmail(), role.getName());
        return new UserDto(saved);
    }

    @Transactional
    public UserDto updateStaffUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getEnabled()   != null) user.setEnabled(request.getEnabled());

        if (request.getStaffRoleId() != null) {
            StaffRole role = roleRepository.findById(request.getStaffRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Role not found: " + request.getStaffRoleId()));
            user.setStaffRole(role);
            user.setRoles(rolesForStaffRole(role));
        }

        if (Boolean.TRUE.equals(request.getResetPassword())) {
            String tempPassword = generateTempPassword(user.getEmail());
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setMustChangePassword(true);
            user.setTempPasswordExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
            emailService.sendStaffPasswordResetEmail(user, tempPassword);
            log.info("Password reset for staff user: {}", user.getEmail());
        }

        return new UserDto(userRepository.save(user));
    }

    @Transactional
    public void deleteStaffUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        refreshTokenRepository.revokeAllByUser(user);
        user.setDeleted(true);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        log.info("Staff user {} soft-deleted", id);
    }

    /**
     * Called after successful first login — clears temp password flag.
     */
    @Transactional
    public void clearMustChangePassword(UUID userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setMustChangePassword(false);
            u.setTempPasswordExpiresAt(null);
            userRepository.save(u);
        });
    }

    /**
     * Generates temp password: MP-{first 4 chars of local email part}{3-digit number}.
     * e.g. email=mdau910@gmail.com → MP-mdau005
     */
    private String generateTempPassword(String email) {
        String localPart = email.contains("@")
                ? email.substring(0, email.indexOf("@"))
                : email;
        String prefix = localPart.length() >= 4
                ? localPart.substring(0, 4).toLowerCase()
                : localPart.toLowerCase();
        int suffix = (int)(Math.random() * 900) + 100; // 100–999
        return "MP-" + prefix + suffix;
    }

    /**
     * Legacy Role gate mirrors the assigned StaffRole: SUPER_ADMIN/ADMIN staff
     * roles get ROLE_ADMIN, everyone else gets ROLE_STAFF. Keeps the old
     * roles-based checks (frontend isAdmin, @IsAdmin) consistent with the
     * fine-grained StaffRole/permission a staff member actually holds.
     */
    private Set<Role> rolesForStaffRole(StaffRole role) {
        if (role != null && ("SUPER_ADMIN".equals(role.getName()) || "ADMIN".equals(role.getName()))) {
            return Set.of(Role.ROLE_ADMIN);
        }
        return Set.of(Role.ROLE_STAFF);
    }
}