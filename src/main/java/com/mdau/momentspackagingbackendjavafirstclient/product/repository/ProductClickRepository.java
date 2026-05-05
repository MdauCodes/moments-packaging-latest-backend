package com.mdau.momentspackagingbackendjavafirstclient.product.repository;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.ProductClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ProductClickRepository extends JpaRepository<ProductClick, UUID> {

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.monthlyClicks = (
            SELECT COUNT(pc) FROM ProductClick pc
            WHERE pc.product = p
            AND pc.clickedAt >= :since
        )
        WHERE p.id IN (
            SELECT DISTINCT pc2.product.id FROM ProductClick pc2
            WHERE pc2.clickedAt >= :since
        )
        """)
    void rollUpMonthlyClicks(Instant since);

    @Modifying
    @Query("DELETE FROM ProductClick pc WHERE pc.clickedAt < :before")
    void deleteOlderThan(Instant before);
}