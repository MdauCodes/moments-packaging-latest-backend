package com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    @Query("""
        SELECT e FROM Enquiry e
        WHERE (:status IS NULL OR e.status = :status)
        ORDER BY e.createdAt DESC
        """)
    Page<Enquiry> findAllWithFilters(
            @Param("status") EnquiryStatus status,
            Pageable pageable
    );
}