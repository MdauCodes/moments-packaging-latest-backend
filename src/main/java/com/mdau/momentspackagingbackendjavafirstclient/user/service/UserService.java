package com.mdau.momentspackagingbackendjavafirstclient.user.service;

import com.mdau.momentspackagingbackendjavafirstclient.auth.repository.RefreshTokenRepository;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ConflictException;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.dto.UserUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import com.mdau.momentspackagingbackendjavafirstclient.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findByDeletedFalse(Pageable.unpaged())
                .stream()
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findByDeletedFalse(pageable).map(UserDto::new);
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userRepository.findById(id)
                .map(UserDto::new)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new ConflictException("Email already in use: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .deleted(false)
                .roles(request.getRoles())
                .build();

        return new UserDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getEnabled()   != null) user.setEnabled(request.getEnabled());
        if (request.getRoles()     != null) user.setRoles(request.getRoles());
        if (request.getPassword()  != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return new UserDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        refreshTokenRepository.revokeAllByUser(user);

        user.setDeleted(true);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        log.info("User {} soft-deleted", id);
    }
}