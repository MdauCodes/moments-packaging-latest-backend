package com.mdau.momentspackagingbackendjavafirstclient.referral.repository;

import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransaction;
import com.mdau.momentspackagingbackendjavafirstclient.referral.entity.CreditTransactionType;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    Page<CreditTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    boolean existsByUserAndType(User user, CreditTransactionType type);
    Page<CreditTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Single row: [0] = total points earned (sum of positive amounts), [1] = total points redeemed (sum of |negative amounts|). */
    @Query("SELECT COALESCE(SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN t.amount < 0 THEN -t.amount ELSE 0 END), 0) FROM CreditTransaction t")
    List<Object[]> sumEarnedAndRedeemed();

    /**
     * Analytics Phase 3 — every transaction type's activity within a date range, as absolute
     * coupon amounts: [0]=type, [1]=count, [2]=total coupons (abs). The service buckets EARNED_*
     * types as "earned by source" and REDEEMED as the program's actual cost for the period;
     * REFUNDED/ADJUSTED/EXPIRED are informational and shown separately if non-zero.
     */
    @Query("""
        SELECT t.type, COUNT(t), COALESCE(SUM(ABS(t.amount)), 0) FROM CreditTransaction t
        WHERE t.createdAt >= :start AND t.createdAt < :end
        GROUP BY t.type
        """)
    List<Object[]> sumByTypeInRange(@Param("start") Instant start, @Param("end") Instant end);
}