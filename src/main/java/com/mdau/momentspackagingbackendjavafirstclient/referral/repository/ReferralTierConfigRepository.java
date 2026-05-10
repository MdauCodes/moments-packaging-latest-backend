package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralTierConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralTierConfigRepository extends JpaRepository<ReferralTierConfig, UUID> {

    List<ReferralTierConfig> findByIsActiveTrueOrderBySortOrderAsc();

    @Query("""
        SELECT t FROM ReferralTierConfig t
        WHERE t.isActive = true
        AND t.minOrderAmount <= :amount
        AND (t.maxOrderAmount IS NULL OR t.maxOrderAmount >= :amount)
        ORDER BY t.minOrderAmount DESC
    """)
    Optional<ReferralTierConfig> findMatchingTier(@Param("amount") BigDecimal amount);
}