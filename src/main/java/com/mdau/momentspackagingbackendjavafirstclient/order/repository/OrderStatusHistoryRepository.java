package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(UUID orderId);

    /**
     * Analytics — every status transition for orders CREATED within the range, as scalars
     * ([0]=orderId, [1]=toStatus, [2]=changedAt) rather than full entities to avoid an N+1 lazy
     * load per row. Ordered so consecutive rows for the same order are adjacent and time-ordered;
     * AnalyticsService turns consecutive changedAt deltas into "time spent in stage X" per order,
     * in Java rather than DB-specific window functions.
     */
    @Query("""
        SELECT h.order.id, h.toStatus, h.changedAt FROM OrderStatusHistory h
        WHERE h.order.createdAt >= :start AND h.order.createdAt < :end
        ORDER BY h.order.id, h.changedAt
        """)
    List<Object[]> findForOrdersCreatedInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Analytics — same shape as findForOrdersCreatedInRange plus [1]=fulfillmentType, so delivery
     *  performance can bucket DISPATCHED→DELIVERED durations per fulfillment type without a second
     *  per-order lookup. Still one query, same ordering guarantee. */
    @Query("""
        SELECT h.order.id, h.order.fulfillmentType, h.toStatus, h.changedAt FROM OrderStatusHistory h
        WHERE h.order.createdAt >= :start AND h.order.createdAt < :end
        ORDER BY h.order.id, h.changedAt
        """)
    List<Object[]> findForOrdersCreatedInRangeWithFulfillmentType(@Param("start") Instant start, @Param("end") Instant end);
}