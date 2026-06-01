package com.mdau.momentspackagingbackendjavafirstclient.common.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public @interface IsSuperAdmin {
}