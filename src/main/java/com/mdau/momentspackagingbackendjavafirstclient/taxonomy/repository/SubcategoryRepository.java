package com.mdau.momentspackagingbackendjavafirstclient.taxonomy.repository;

import com.mdau.momentspackagingbackendjavafirstclient.taxonomy.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, UUID> {

    Optional<Subcategory> findByCategoryIdAndSlug(UUID categoryId, String slug);

    boolean existsByNameAndCategoryId(String name, UUID categoryId);

    List<Subcategory> findByCategoryId(UUID categoryId);

    List<Subcategory> findByCategoryIdIn(Collection<UUID> categoryIds);

    long countByCategoryId(UUID categoryId);

    List<Subcategory> findByIndustries_Id(UUID industryId);
}
