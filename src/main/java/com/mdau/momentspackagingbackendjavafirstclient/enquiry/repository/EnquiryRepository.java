package com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus;
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
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    @Query("""
        SELECT e FROM Enquiry e
        WHERE (:source IS NULL OR e.source = :source)
        AND   (:status IS NULL OR e.status = :status)
        AND   (:assignedToId IS NULL OR e.assignedToId = :assignedToId)
        AND   (:q IS NULL
               OR LOWER(e.email)       LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.contactName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.company)     LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY e.createdAt DESC
        """)
    Page<Enquiry> findAllWithFilters(
            @Param("source")       String source,
            @Param("status")       EnquiryStatus status,
            @Param("assignedToId") UUID assignedToId,
            @Param("q")            String q,
            Pageable pageable);

    /** Enquiries due for follow-up that are not yet won/lost/archived. */
    @Query("""
        SELECT e FROM Enquiry e
        WHERE e.followUpAt <= :now
        AND e.status NOT IN (
            com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus.WON,
            com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus.LOST,
            com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryStatus.ARCHIVED)
        ORDER BY e.followUpAt ASC
        """)
    List<Enquiry> findDueFollowUps(@Param("now") Instant now);

    /** Pipeline summary count by status — used by dashboard. */
    @Query("""
        SELECT e.status, COUNT(e) FROM Enquiry e
        GROUP BY e.status
        """)
    List<Object[]> countByStatus();
}