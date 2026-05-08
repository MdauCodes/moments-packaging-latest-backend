package com.mdau.momentspackagingbackendjavafirstclient.auth.repository;

import com.mdau.momentspackagingbackendjavafirstclient.auth.entity.PasswordResetToken;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    /** Returns ALL unused tokens for a user — caller must handle list */
    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.used = true")
    void deleteExpiredAndUsed(Instant now);
}