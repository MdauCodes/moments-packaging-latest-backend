package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InvoiceCounter c WHERE c.year = :year")
    Optional<InvoiceCounter> findByYearForUpdate(@Param("year") String year);
}
