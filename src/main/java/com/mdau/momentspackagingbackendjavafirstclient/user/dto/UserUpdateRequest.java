package com.mdau.momentspackagingbackendjavafirstclient.user.dto;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserUpdateRequest {

    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Set<Role> roles;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}