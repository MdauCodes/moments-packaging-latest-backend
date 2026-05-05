package com.mdau.momentspackagingbackendjavafirstclient.lead.repository;

import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT l FROM Lead l
        WHERE l.createdAt >= :since
        AND l.contacted = false
        ORDER BY l.createdAt DESC
        """)
    List<Lead> findRecentUncontacted(@Param("since") Instant since);
}