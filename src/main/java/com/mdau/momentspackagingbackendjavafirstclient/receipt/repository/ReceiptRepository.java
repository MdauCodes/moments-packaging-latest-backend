package com.mdau.momentspackagingbackendjavafirstclient.receipt.repository;

import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.Receipt;
import com.mdau.momentspackagingbackendjavafirstclient.receipt.entity.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    Optional<Receipt> findByOrder_Reference(String orderReference);
    Page<Receipt> findByStatus(ReceiptStatus status, Pageable pageable);
}
