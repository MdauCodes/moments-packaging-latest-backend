package com.mdau.momentspackagingbackendjavafirstclient.user.service;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.superadmin-password}")
    private String superadminPassword;

    @Value("${app.admin.dev-admin-password}")
    private String devAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAdmin("pkihara2008@gmail.com", "Peter",  "Kihara",    superadminPassword);
        seedAdmin("mdaucodes@gmail.com",   "Mdau",   "Developer", devAdminPassword);
    }

    private void seedAdmin(String email, String firstName, String lastName, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            log.info("Admin account already exists, skipping: {}", email);
            return;
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            log.warn("Skipping seed for {} — password not configured.", email);
            return;
        }
        User admin = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_STAFF))
                .build();
        userRepository.save(admin);
        log.info("Seeded admin account: {}", email);
    }
}