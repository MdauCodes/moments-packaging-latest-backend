package com.mdau.momentspackagingbackendjavafirstclient.auth.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.AccountType;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
public class AuthUserDto {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final Set<Role> roles;
    private final AccountType accountType;

    public AuthUserDto(User user) {
        this.id          = user.getId();
        this.email       = user.getEmail();
        this.firstName   = user.getFirstName();
        this.lastName    = user.getLastName();
        this.roles       = user.getRoles();
        this.accountType = user.getAccountType();
    }
}