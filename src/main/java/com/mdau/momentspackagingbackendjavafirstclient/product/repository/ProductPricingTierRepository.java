package com.mdau.momentspackagingbackendjavafirstclient.product.repository;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductPricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductPricingTierRepository extends JpaRepository<ProductPricingTier, UUID> {
    List<ProductPricingTier> findByProductId(UUID productId);
    void deleteByProductId(UUID productId);
}