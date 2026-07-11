package com.mdau.momentspackagingbackendjavafirstclient.order.repository;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
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

    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0), MAX(o.createdAt) FROM Order o
        WHERE o.customer = :customer AND o.paymentStatus = 'PAID'
        """)
    List<Object[]> getOrderStatsForCustomer(@Param("customer") User customer);

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