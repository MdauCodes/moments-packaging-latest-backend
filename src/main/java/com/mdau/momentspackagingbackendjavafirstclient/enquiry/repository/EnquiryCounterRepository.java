package com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnquiryCounterRepository extends JpaRepository<EnquiryCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM EnquiryCounter c WHERE c.yearMonth = :yearMonth")
    Optional<EnquiryCounter> findByYearMonthForUpdate(@Param("yearMonth") String yearMonth);
}