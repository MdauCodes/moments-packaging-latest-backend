package com.mdau.momentspackagingbackendjavafirstclient.user.repository;

import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Security + auth — only loads active non-deleted users */
    Optional<User> findByEmailAndDeletedFalse(String email);

    /** Registration uniqueness — ignores deleted accounts */
    boolean existsByEmailAndDeletedFalse(String email);

    /** Seeder + admin lookup — checks any account including deleted */
    boolean existsByEmail(String email);

    /** Admin: find any user by email including deleted */
    Optional<User> findByEmail(String email);

    /** Admin: paginated list of non-deleted users */
    Page<User> findByDeletedFalse(Pageable pageable);

    /** Admin: search non-deleted users by email or name */
    @Query("""
        SELECT u FROM User u
        WHERE u.deleted = false
        AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<User> searchActiveUsers(String q, Pageable pageable);
}