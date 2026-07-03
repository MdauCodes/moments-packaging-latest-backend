package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SegmentRepository extends JpaRepository<Segment, UUID> {

    Optional<Segment> findBySlug(String slug);

    boolean existsByName(String name);
}
