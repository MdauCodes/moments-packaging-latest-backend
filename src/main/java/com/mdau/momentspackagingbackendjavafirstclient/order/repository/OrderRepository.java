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

    /** Analytics Phase 4 — tax report: [0]=sum(taxableAmount), [1]=sum(vatAmount), [2]=paid order
     *  count, for PAID orders in range. taxableAmount is already the post-discount KRA base and
     *  vatAmount the VAT already extracted from it (see Order.java) — this is the amount to remit. */
    @Query("""
        SELECT COALESCE(SUM(o.taxableAmount), 0), COALESCE(SUM(o.vatAmount), 0), COUNT(o) FROM Order o
        WHERE o.paymentStatus = 'PAID'
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    List<Object[]> sumVatForPaidInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Count of PAID orders in range that requested a tax invoice / ETR bundle — for compliance stats. */
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.paymentStatus = 'PAID' AND o.taxInvoiceRequested = true
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    long countTaxInvoiceRequestedInRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.paymentStatus = 'PAID' AND o.etrRequested = true
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    long countEtrRequestedInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Analytics Phase 5 — top sellers for a date range: [0]=name, [1]=units sold, [2]=revenue. */
    @Query("""
        SELECT oi.productNameSnapshot, COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.lineTotal), 0)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.paymentStatus = 'PAID'
        AND o.createdAt >= :start AND o.createdAt < :end
        GROUP BY oi.productNameSnapshot
        ORDER BY SUM(oi.lineTotal) DESC
        """)
    List<Object[]> findTopSellingProductsInRange(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    /** Analytics Phase 6 — units sold per product for a date range (PAID orders only): [0]=productId,
     *  [1]=units. productId can be null for legacy/manual line items, which the service treats as
     *  "cost unknown" rather than silently dropping from the units-sold count. */
    @Query("""
        SELECT oi.productId, COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.paymentStatus = 'PAID'
        AND o.createdAt >= :start AND o.createdAt < :end
        GROUP BY oi.productId
        """)
    List<Object[]> sumUnitsSoldByProductInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Analytics Phase 8 — new vs returning: [0]=customerId, [1]=lifetime PAID order count, [2]=lifetime
     *  PAID revenue, for customers whose FIRST paid order fell in this range (i.e. they're "new" here). */
    @Query("""
        SELECT o.customer.id, COUNT(o), COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.customer IS NOT NULL AND o.paymentStatus = 'PAID'
        GROUP BY o.customer.id
        HAVING MIN(o.createdAt) >= :start AND MIN(o.createdAt) < :end
        """)
    List<Object[]> findNewPaidCustomersInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Analytics Phase 8 — top customers by lifetime PAID revenue: [0]=customerId, [1]=firstName,
     *  [2]=lastName, [3]=accountType, [4]=order count, [5]=lifetime revenue. */
    @Query("""
        SELECT o.customer.id, o.customer.firstName, o.customer.lastName, o.customer.accountType,
               COUNT(o), COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.customer IS NOT NULL AND o.paymentStatus = 'PAID'
        GROUP BY o.customer.id, o.customer.firstName, o.customer.lastName, o.customer.accountType
        ORDER BY SUM(o.totalAmount) DESC
        """)
    List<Object[]> findTopCustomersByLifetimeRevenue(Pageable pageable);

    /** Analytics Phase 8 — PAID revenue in range grouped by the customer's account type. */
    @Query("""
        SELECT o.customer.accountType, COUNT(DISTINCT o.customer.id), COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.customer IS NOT NULL AND o.paymentStatus = 'PAID'
        AND o.createdAt >= :start AND o.createdAt < :end
        GROUP BY o.customer.accountType
        """)
    List<Object[]> sumRevenueByAccountTypeInRange(@Param("start") Instant start, @Param("end") Instant end);

    /** Analytics charts — daily revenue trend line: [0]=createdAt, [1]=totalAmount, [2]=paymentStatus,
     *  for every order in range whose payment status is one of the three the trend chart plots.
     *  Bucketed by local calendar day in Java (Africa/Nairobi), same approach as the Phase 2
     *  time-in-stage math — no DB-side date_trunc, kept portable and consistent with the rest of
     *  this service. */
    @Query("""
        SELECT o.createdAt, o.totalAmount, o.paymentStatus FROM Order o
        WHERE o.paymentStatus IN ('PAID', 'PENDING', 'FAILED')
        AND o.createdAt >= :start AND o.createdAt < :end
        """)
    List<Object[]> findRevenueTrendRowsInRange(@Param("start") Instant start, @Param("end") Instant end);
}