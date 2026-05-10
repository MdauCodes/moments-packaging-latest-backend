package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransaction;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    Page<CreditTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<CreditTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}