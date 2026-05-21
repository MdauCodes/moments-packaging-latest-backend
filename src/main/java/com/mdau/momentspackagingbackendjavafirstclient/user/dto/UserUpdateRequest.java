package com.mdau.momentspackagingbackendjavafirstclient.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserUpdateRequest {

    private String  firstName;
    private String  lastName;
    private Boolean enabled;
    private UUID    staffRoleId;

    /** Only used by SUPER_ADMIN for password reset — generates new temp password */
    private Boolean resetPassword;
}