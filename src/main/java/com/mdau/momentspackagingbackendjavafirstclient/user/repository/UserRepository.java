package com.mdau.momentspackagingbackendjavafirstclient.user.repository;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    Page<User> findByDeletedFalse(Pageable pageable);

    /** Staff users only — excludes customers. Used in all admin user management. */
    Page<User> findByIsStaffTrueAndDeletedFalse(Pageable pageable);

    /** All non-deleted staff — used for order assignment dropdown. */
    List<User> findByIsStaffTrueAndDeletedFalseAndEnabledTrue();

    /** Find expired temp-password accounts that were never activated. */
    List<User> findByMustChangePasswordTrueAndTempPasswordExpiresAtBeforeAndDeletedFalse(
            Instant now);
}