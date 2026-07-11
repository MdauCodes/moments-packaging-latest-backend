package com.mdau.momentspackagingbackendjavafirstclient.user.service;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Permission;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.StaffRole;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.StaffRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class StaffRoleSeeder implements ApplicationRunner {

    private final StaffRoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        seed("SUPER_ADMIN", "Super Admin",
                "Full system access including role and user management.",
                Set.of(Permission.values())); // all permissions

        seed("ADMIN", "Admin",
                "Full access except role management.",
                Set.of(
                        Permission.ORDER_VIEW, Permission.ORDER_VERIFY_PAYMENT,
                        Permission.ORDER_PREPARE, Permission.ORDER_DISPATCH,
                        Permission.ORDER_ASSIGN, Permission.ORDER_MANAGE_ALL,
                        Permission.PRODUCT_VIEW, Permission.PRODUCT_MANAGE,
                        Permission.PAYMENT_VIEW, Permission.PAYMENT_REFUND,
                        Permission.USER_VIEW, Permission.USER_CREATE,
                        Permission.ANALYTICS_VIEW, Permission.SETTINGS_MANAGE,
                        Permission.BLOG_MANAGE, Permission.ENQUIRY_VIEW,
                        Permission.REVIEW_MODERATE, Permission.CUSTOMER_VIEW));

        seed("SUPERVISOR", "Supervisor",
                "View all orders and assign them to staff.",
                Set.of(
                        Permission.ORDER_VIEW, Permission.ORDER_ASSIGN,
                        Permission.ORDER_MANAGE_ALL, Permission.ANALYTICS_VIEW,
                        Permission.USER_VIEW));

        seed("PAYMENTS_CONFIRMER", "Payments Confirmer",
                "Verify payments and move orders to preparation.",
                Set.of(
                        Permission.ORDER_VIEW, Permission.ORDER_VERIFY_PAYMENT,
                        Permission.PAYMENT_VIEW));

        seed("PREPARER", "Preparer / Packager",
                "Package orders and mark them ready for dispatch.",
                Set.of(
                        Permission.ORDER_VIEW, Permission.ORDER_PREPARE));

        seed("DISPATCHER", "Dispatcher",
                "Verify order contents and dispatch to courier.",
                Set.of(
                        Permission.ORDER_VIEW, Permission.ORDER_DISPATCH));

        seed("STAFF", "Staff",
                "Basic staff — permissions assigned individually by supervisor.",
                Set.of(Permission.ORDER_VIEW));

        log.info("Staff role seeding complete.");
    }

    /**
     * Creates the role if missing. If it already exists AND is still a
     * default (never customized by a SUPER_ADMIN), its permission set is
     * refreshed to match the list defined here — otherwise a role seeded
     * before a new Permission value existed would never receive it, since
     * this runner only used to act on first creation.
     */
    private void seed(String name, String displayName,
                      String description, Set<Permission> permissions) {
        roleRepository.findByNameAndDeletedFalse(name).ifPresentOrElse(
                existing -> {
                    if (Boolean.TRUE.equals(existing.getIsDefault())
                            && !existing.getPermissions().equals(permissions)) {
                        // Mutate the managed Hibernate collection in place —
                        // replacing the field with an immutable Set.of() breaks
                        // @ElementCollection dirty-checking.
                        existing.getPermissions().clear();
                        existing.getPermissions().addAll(permissions);
                        roleRepository.save(existing);
                        log.info("Refreshed permissions for default role: {}", name);
                    }
                },
                () -> {
                    roleRepository.save(StaffRole.builder()
                            .name(name)
                            .displayName(displayName)
                            .description(description)
                            .isDefault(true)
                            .permissions(permissions)
                            .deleted(false)
                            .build());
                    log.info("Seeded role: {}", name);
                });
    }
}