package com.mdau.momentspackagingbackendjavafirstclient.product.repository;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
        LEFT JOIN p.curatedTags t
        WHERE p.deleted = false
        AND p.risellerSuspended = false
        AND (:industryId IS NULL OR i.id = :industryId)
        AND (:tagId IS NULL OR t.id = :tagId)
        AND (:isDiscount IS NULL OR p.isDiscount = :isDiscount)
        AND (:isNewArrival IS NULL OR p.isNewArrival = :isNewArrival)
        AND (:isFastMoving IS NULL OR p.isFastMoving = :isFastMoving)
        AND (:category IS NULL OR LOWER(p.category) = LOWER(CAST(:category AS string)))
        AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
        """)
    Page<Product> findAllWithFilters(
            @Param("industryId")     UUID industryId,
            @Param("tagId")          UUID tagId,
            @Param("isDiscount")     Boolean isDiscount,
            @Param("isNewArrival")   Boolean isNewArrival,
            @Param("isFastMoving")   Boolean isFastMoving,
            @Param("category")      String category,
            @Param("subcategoryId") UUID subcategoryId,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.risellerSuspended = false ORDER BY p.monthlyClicks DESC")
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
    SELECT p FROM Product p
    LEFT JOIN FETCH p.imageUrls
    WHERE p.deleted = false AND p.risellerSuspended = false
    """)
    List<Product> findAllActive();

    /** Products suspended because Riseller dropped them — kept for admin content preservation. */
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.risellerSuspended = true")
    List<Product> findSuspended();

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.stockCount = p.stockCount - :units,
            p.stockStatus = CASE
                WHEN (p.stockCount - :units) <= 0 THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.OUT_OF_STOCK
                WHEN (p.stockCount - :units) <= p.lowStockThreshold THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.LOW_STOCK
                ELSE com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.IN_STOCK
            END
        WHERE p.id = :productId
        AND p.stockCount >= :units
        """)
    int deductStock(@Param("productId") UUID productId, @Param("units") int units);

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.stockCount = p.stockCount + :units,
            p.stockStatus = CASE
                WHEN (p.stockCount + :units) <= 0 THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.OUT_OF_STOCK
                WHEN (p.stockCount + :units) <= p.lowStockThreshold THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.LOW_STOCK
                ELSE com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.IN_STOCK
            END
        WHERE p.id = :productId
        """)
    void restoreStock(@Param("productId") UUID productId, @Param("units") int units);

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.stockCount = :newCount,
            p.stockStatus = CASE
                WHEN :newCount <= 0 THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.OUT_OF_STOCK
                WHEN :newCount <= p.lowStockThreshold THEN com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.LOW_STOCK
                ELSE com.mdau.momentspackagingbackendjavafirstclient.product.entity.StockStatus.IN_STOCK
            END
        WHERE p.id = :productId
        """)
    void setStockCount(@Param("productId") UUID productId, @Param("newCount") int newCount);

    List<Product> findByStockStatusAndDeletedFalse(StockStatus stockStatus);

    boolean existsBySlug(String slug);

    @Query("SELECT p FROM Product p JOIN p.keywords k WHERE k = :keyword AND p.deleted = false")
    java.util.Optional<com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product> findByKeywordAndDeletedFalse(@Param("keyword") String keyword);


    Optional<Product> findByRisellerItemIdAndDeletedFalse(String risellerItemId);

    /**
     * Products not yet linked to any Riseller item — candidates for auto-matching.
     * Includes suspended products so they can be re-activated when Riseller re-lists them.
     */
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND (p.risellerItemId IS NULL OR p.risellerItemId = '')")
    List<Product> findUnlinkedProducts();

    /**
     * Products whose Riseller ID is set but no longer appears in the current catalog —
     * i.e., Riseller removed the item.
     */
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.risellerItemId IS NOT NULL AND p.risellerItemId NOT IN :validIds")
    List<Product> findOrphanedByRisellerId(@Param("validIds") Collection<String> validIds);

    long countBySubcategoryIdAndDeletedFalse(UUID subcategoryId);

    List<Product> findBySubcategoryIdAndDeletedFalse(UUID subcategoryId);

    List<Product> findBySubcategoryIdInAndDeletedFalse(Collection<UUID> subcategoryIds);

    List<Product> findByDeletedFalse();

    List<Product> findByCategoryAndDeletedFalseAndSubcategoryIsNull(String category);

}