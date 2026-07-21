package com.mdau.momentspackagingbackendjavafirstclient.payment.repository;

import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    @Query("SELECT p FROM PaymentRecord p WHERE p.order.id = :orderId ORDER BY p.createdAt DESC")
    List<PaymentRecord> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") UUID orderId);

    Optional<PaymentRecord> findByCheckoutRequestId(String checkoutRequestId);
    Optional<PaymentRecord> findByExternalReference(String externalReference);

    /**
     * Analytics — payment attempts (not orders; one order can have several, e.g. an STK retry)
     * grouped by method and status within a date range. [0]=method, [1]=status, [2]=count.
     * Powers the "payment success rate by method" breakdown.
     */
    @Query("""
        SELECT p.method, p.status, COUNT(p) FROM PaymentRecord p
        WHERE p.createdAt >= :start AND p.createdAt < :end
        GROUP BY p.method, p.status
        """)
    List<Object[]> countByMethodAndStatusInRange(@Param("start") Instant start, @Param("end") Instant end);
}