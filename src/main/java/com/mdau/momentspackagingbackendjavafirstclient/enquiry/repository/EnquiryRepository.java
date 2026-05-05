package com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {
}