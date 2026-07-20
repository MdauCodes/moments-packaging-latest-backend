package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.repository;

import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentBundleRepository extends JpaRepository<DocumentBundle, UUID> {
    Optional<DocumentBundle> findByOrder_Reference(String orderReference);
    Page<DocumentBundle> findByStatus(DocumentBundleStatus status, Pageable pageable);
}
