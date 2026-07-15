package com.mdau.momentspackagingbackendjavafirstclient.taxdocument.repository;

import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocument;
import com.mdau.momentspackagingbackendjavafirstclient.taxdocument.entity.TaxDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxDocumentRepository extends JpaRepository<TaxDocument, UUID> {

    Page<TaxDocument> findByStatus(TaxDocumentStatus status, Pageable pageable);

    Optional<TaxDocument> findByOrder_Reference(String orderReference);

    List<TaxDocument> findByStatusAndCreatedAtBefore(TaxDocumentStatus status, Instant cutoff);
}
