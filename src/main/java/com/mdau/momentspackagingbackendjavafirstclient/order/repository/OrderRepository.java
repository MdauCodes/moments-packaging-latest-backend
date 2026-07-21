package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.PaymentStatus;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByReference(String reference);

    Page<Order> findByCustomerOrderByCreatedAtDesc(User customer, Pageable pageable);

    boolean existsByReference(String reference);

    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
        AND (:customerId IS NULL OR o.customer.id = :customerId)
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("customerId") UUID customerId,
            Pageable pageable);

    /**
     * Public email-based order lookup â€” no auth required.
     * Returns all orders placed with this email, newest first.
     * Used by the track-by-email feature.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE LOWER(o.email) = LOWER(:email)
        ORDER BY o.createdAt DESC
        """)
    Page<Order> findByEmailIgnoreCaseOrderByCreatedAtDesc(
            @Param("email") String email,
            Pageable pageable);

    /**
     * Idempotency check â€” find existing order by idempotency key.
     * The key is stored in the notes field prefixed with "idem:" during checkout.
     */
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.paymentStatus = 'PAID'
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    BigDecimal sumPaidOrdersInRange(@Param("start") Instant start, @Param("end") Instant end);

    long countByStatus(OrderStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    /**
     * Analytics dashboard — value of orders in a date range by payment status, generalized
     * beyond sumPaidOrdersInRange (kept as-is since other callers depend on its exact signature).
     * Revenue must only ever come from this with status=PAID — PENDING/FAILED rows are real
     * orders but never actual revenue.
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.paymentStatus = :status
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    BigDecimal sumOrdersInRangeByPaymentStatus(
            @Param("start") Instant start, @Param("end") Instant end, @Param("status") PaymentStatus status);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.paymentStatus = :status
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    long countOrdersInRangeByPaymentStatus(
            @Param("start") Instant start, @Param("end") Instant end, @Param("status") PaymentStatus status);

    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0), MAX(o.createdAt) FROM Order o
        WHERE o.customer = :customer AND o.paymentStatus = 'PAID'
        """)
    List<Object[]> getOrderStatsForCustomer(@Param("customer") User customer);

    /** Analytics Phase 2 — order status funnel: [0]=status, [1]=count, for orders created in range. */
    @Query("""
        SELECT o.status, COUNT(o) FROM Order o
        WHERE o.createdAt >= :start AND o.createdAt < :end
        GROUP BY o.status
        """)
    List<Object[]> countByStatusInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Distinct registered customers who placed an order in range — the cohort for repeat-rate. */
    @Query("""
        SELECT DISTINCT o.customer.id FROM Order o
        WHERE o.customer IS NOT NULL AND o.createdAt >= :start AND o.createdAt < :end
        """)
    List<UUID> findDistinctCustomerIdsInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Lifetime PAID order count per customer, for the given ids — [0]=customerId, [1]=count. */
    @Query("""
        SELECT o.customer.id, COUNT(o) FROM Order o
        WHERE o.customer.id IN :customerIds AND o.paymentStatus = 'PAID'
        GROUP BY o.customer.id
        """)
    List<Object[]> countLifetimePaidOrdersByCustomerIds(@Param("customerIds") List<UUID> customerIds);

    /** Refund requests logged in range — [0]=count, [1]=value of the affected orders. */
    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.refundRequestedAt >= :start AND o.refundRequestedAt < :end
        """)
    List<Object[]> refundsRequestedInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Refund requests resolved in range, with both timestamps, for computing average resolution time in Java. */
    @Query("""
        SELECT o FROM Order o
        WHERE o.refundResolvedAt >= :start AND o.refundResolvedAt < :end AND o.refundRequestedAt IS NOT NULL
        """)
    List<Order> findResolvedRefundsInRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0), MIN(o.createdAt), MAX(o.createdAt) FROM Order o
        WHERE o.customer = :customer AND o.paymentStatus = 'PAID'
        """)
    List<Object[]> getOrderSummaryForCustomer(@Param("customer") User customer);

    @Query("""
        SELECT oi.productNameSnapshot, SUM(oi.lineTotal) as revenue
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.paymentStatus = 'PAID'
        AND o.createdAt >= :start
        GROUP BY oi.productNameSnapshot
        ORDER BY revenue DESC
        """)
    List<Object[]> findTopSellingProducts(@Param("start") Instant start, Pageable pageable);
}