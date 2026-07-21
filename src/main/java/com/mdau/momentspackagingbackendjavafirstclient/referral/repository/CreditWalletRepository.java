package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditWallet;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditWalletRepository extends JpaRepository<CreditWallet, UUID> {
    Optional<CreditWallet> findByUser(User user);

    /** Analytics Phase 3 — current outstanding balance across every wallet (a snapshot, not date-range-scoped). */
    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM CreditWallet w")
    long sumAllBalances();

    /** Every current balance, for computing the median in Java (JPQL has no median function). */
    @Query("SELECT w.balance FROM CreditWallet w")
    List<Integer> findAllBalances();

    /** Biggest wallets, for the "top holders" list — [0]=firstName, [1]=lastName, [2]=balance. */
    @Query("SELECT w.user.firstName, w.user.lastName, w.balance FROM CreditWallet w ORDER BY w.balance DESC")
    List<Object[]> findTopHolders(Pageable pageable);
}