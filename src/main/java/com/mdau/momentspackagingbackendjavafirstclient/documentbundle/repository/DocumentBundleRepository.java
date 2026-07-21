package com.mdau.momentspackagingbackendjavafirstclient.documentbundle.repository;

import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundle;
import com.mdau.momentspackagingbackendjavafirstclient.documentbundle.entity.DocumentBundleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentBundleRepository extends JpaRepository<DocumentBundle, UUID> {
    Optional<DocumentBundle> findByOrder_Reference(String orderReference);
    Page<DocumentBundle> findByStatus(DocumentBundleStatus status, Pageable pageable);
    /** Used by the monthly cleanup job — 2 months is measured from when the ETR was uploaded, not from checkout. */
    List<DocumentBundle> findByStatusAndEtrUploadedAtBefore(DocumentBundleStatus status, Instant cutoff);

    /** Analytics Phase 4 — ETR bundle status breakdown for orders created in range: [0]=status, [1]=count. */
    @Query("""
        SELECT b.status, COUNT(b) FROM DocumentBundle b
        WHERE b.order.createdAt >= :start AND b.order.createdAt < :end
        GROUP BY b.status
        """)
    List<Object[]> countByStatusForOrdersInRange(@Param("start") Instant start, @Param("end") Instant end);
}
