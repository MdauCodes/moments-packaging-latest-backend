package com.mdau.momentspackagingbackendjavafirstclient.user.repository;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Customer search for the admin Customers page — excludes staff/admin accounts. */
    @Query("""
        SELECT u FROM User u
        WHERE u.isStaff = false AND u.deleted = false
        AND (:q IS NULL OR :q = ''
             OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
             OR u.phone LIKE CONCAT('%', :q, '%'))
        ORDER BY u.createdAt DESC
        """)
    List<User> searchCustomers(@Param("q") String q);

    /** One-time backfill for accounts created before AccountType existed. */
    @Modifying
    @Query("""
        UPDATE User u SET u.accountType = com.mdau.momentspackagingbackendjavafirstclient.user.entity.AccountType.SOLE_MERCHANT
        WHERE u.accountType IS NULL AND u.isStaff = false AND u.deleted = false
        """)
    int backfillMissingAccountTypeAsSoleMerchant();
}