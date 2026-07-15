package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySegmentIdAndSlug(UUID segmentId, String slug);

    boolean existsByNameAndSegmentId(String name, UUID segmentId);

    List<Category> findBySegmentId(UUID segmentId);

    long countBySegmentId(UUID segmentId);

    List<Category> findByIndustries_Id(UUID industryId);
}
