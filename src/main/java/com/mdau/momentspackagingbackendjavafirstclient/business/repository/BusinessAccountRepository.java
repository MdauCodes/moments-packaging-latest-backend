package com.mdau.momentspackagingbackendjavafirstclient.business.repository;

import com.mdau.momentspackagingbackendjavafirstclient.business.entity.BusinessAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessAccountRepository extends JpaRepository<BusinessAccount, UUID> {

    Optional<BusinessAccount> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<BusinessAccount> findByBusinessNameContainingIgnoreCaseOrKraPinContainingIgnoreCase(
            String businessName, String kraPin, Pageable pageable);
}
