package com.mdau.momentspackagingbackendjavafirstclient.industry.repository;

import com.mdau.momentspackagingbackendjavafirstclient.industry.entity.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    Optional<Industry> findBySlug(String slug);

    boolean existsByName(String name);
}