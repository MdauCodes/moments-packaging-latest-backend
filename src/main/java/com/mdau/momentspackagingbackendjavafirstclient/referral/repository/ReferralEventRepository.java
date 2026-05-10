package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralEvent;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.ReferralEventStatus;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralEventRepository extends JpaRepository<ReferralEvent, UUID> {
    Optional<ReferralEvent> findByRefereeAndStatus(User referee, ReferralEventStatus status);
    List<ReferralEvent> findByReferrerOrderByCreatedAtDesc(User referrer);
    Page<ReferralEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByReferrerAndStatus(User referrer, ReferralEventStatus status);
}