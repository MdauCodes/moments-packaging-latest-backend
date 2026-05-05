package com.mdau.momentspackagingbackendjavafirstclient.payment.repository;

import com.mdau.momentspackagingbackendjavafirstclient.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    @Query("SELECT p FROM PaymentRecord p WHERE p.order.id = :orderId ORDER BY p.createdAt DESC")
    List<PaymentRecord> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") UUID orderId);

    Optional<PaymentRecord> findByCheckoutRequestId(String checkoutRequestId);
    Optional<PaymentRecord> findByExternalReference(String externalReference);
}