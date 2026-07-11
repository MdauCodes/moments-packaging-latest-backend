package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.RewardsTierConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RewardsTierConfigRepository extends JpaRepository<RewardsTierConfig, UUID> {

    List<RewardsTierConfig> findByIsActiveTrueOrderBySortOrderAsc();

    /** Highest tier a merchant with this many lifetime points qualifies for. */
    @Query("""
        SELECT t FROM RewardsTierConfig t
        WHERE t.isActive = true AND t.minLifetimePoints <= :lifetimePoints
        ORDER BY t.minLifetimePoints DESC
    """)
    List<RewardsTierConfig> findQualifyingTiersDesc(@Param("lifetimePoints") int lifetimePoints);

    default Optional<RewardsTierConfig> findCurrentTier(int lifetimePoints) {
        List<RewardsTierConfig> matches = findQualifyingTiersDesc(lifetimePoints);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }
}
