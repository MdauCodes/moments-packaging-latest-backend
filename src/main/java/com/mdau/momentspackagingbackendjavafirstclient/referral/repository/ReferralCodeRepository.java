package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralCode;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralCodeRepository extends JpaRepository<ReferralCode, UUID> {
    Optional<ReferralCode> findByCode(String code);
    Optional<ReferralCode> findByUser(User user);
    boolean existsByCode(String code);
}