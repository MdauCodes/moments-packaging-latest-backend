package com.mdau.momentspackagingbackendjavafirstclient.product.repository;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlugAndDeletedFalse(String slug);

    Optional<Product> findByIdAndDeletedFalse(UUID id);

    boolean existsBySlugAndDeletedFalse(String slug);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.industries i
        WHERE p.deleted = false
        AND (:industryId IS NULL OR i.id = :industryId)
        AND (:isDiscount IS NULL OR p.isDiscount = :isDiscount)
        AND (:isNewArrival IS NULL OR p.isNewArrival = :isNewArrival)
        AND (:isFastMoving IS NULL OR p.isFastMoving = :isFastMoving)
        AND (:category IS NULL OR LOWER(p.category) = LOWER(CAST(:category AS string)))
        """)
    Page<Product> findAllWithFilters(
            @Param("industryId")   UUID industryId,
            @Param("isDiscount")   Boolean isDiscount,
            @Param("isNewArrival") Boolean isNewArrival,
            @Param("isFastMoving") Boolean isFastMoving,
            @Param("category")     String category,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false ORDER BY p.monthlyClicks DESC")
    List<Product> findTopByMonthlyClicks(Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.monthlyClicks = 0 WHERE p.deleted = false")
    void resetAllMonthlyClicks();

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.monthlyClicks = p.monthlyClicks + 1,
            p.totalClicks   = p.totalClicks + 1
        WHERE p.id = :productId
        """)
    void incrementClicks(@Param("productId") UUID productId);

    @Query("""
        SELECT p FROM Product p
        WHERE p.deleted = false
        AND p.stockCount > 0
        AND p.stockCount <= p.lowStockThreshold
        ORDER BY p.stockCount ASC
        """)
    List<Product> findLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.deleted = false")
    long countActive();

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN FETCH p.imageUrls
    LEFT JOIN FETCH p.sizes
    LEFT JOIN FETCH p.tags
    LEFT JOIN FETCH p.keywords
    LEFT JOIN FETCH p.industries
    WHERE p.deleted = false
    """)
    List<Product> findAllActive();
}