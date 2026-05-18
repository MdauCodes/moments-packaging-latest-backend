package com.mdau.momentspackagingbackendjavafirstclient.product.repository;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductUom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductUomRepository extends JpaRepository<ProductUom, UUID> {

    List<ProductUom> findByDeletedFalseOrderBySortOrderAsc();

    Optional<ProductUom> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);
}