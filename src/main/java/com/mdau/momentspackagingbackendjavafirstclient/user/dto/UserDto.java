package com.mdau.momentspackagingbackendjavafirstclient.user.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
public class UserDto {

    private final UUID      id;
    private final String    email;
    private final String    firstName;
    private final String    lastName;
    private final Boolean   enabled;
    private final Set<Role> roles;
    private final Instant   createdAt;
    private final Instant   updatedAt;

    public UserDto(User user) {
        this.id        = user.getId();
        this.email     = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName  = user.getLastName();
        this.enabled   = user.isEnabled();
        this.roles     = user.getRoles();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}