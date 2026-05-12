package com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    @Query("""
        SELECT e FROM Enquiry e
        WHERE (:source IS NULL OR e.source = :source)
        AND (:q IS NULL OR LOWER(e.email) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(e.contactName) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY e.createdAt DESC
        """)
    Page<Enquiry> findAllWithFilters(String source, String q, Pageable pageable);
}