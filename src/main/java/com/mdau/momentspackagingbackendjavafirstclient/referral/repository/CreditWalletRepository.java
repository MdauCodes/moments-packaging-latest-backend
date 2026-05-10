package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditWallet;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditWalletRepository extends JpaRepository<CreditWallet, UUID> {
    Optional<CreditWallet> findByUser(User user);
}