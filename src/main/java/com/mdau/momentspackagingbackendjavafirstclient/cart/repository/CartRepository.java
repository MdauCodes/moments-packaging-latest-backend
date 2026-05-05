package com.mdau.momentspackagingbackendjavafirstclient.cart.repository;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.Cart;
import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartStatus;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByCustomerAndStatus(User customer, CartStatus status);
    Optional<Cart> findBySessionIdAndStatus(String sessionId, CartStatus status);
    Optional<Cart> findByIdAndCustomer(UUID id, User customer);

    @Modifying
    @Query("""
        UPDATE Cart c SET c.status = 'ABANDONED'
        WHERE c.status = 'ACTIVE'
        AND c.updatedAt < :cutoff
        """)
    int markAbandonedBefore(@Param("cutoff") Instant cutoff);
}