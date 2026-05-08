package com.mdau.momentspackagingbackendjavafirstclient.cart.repository;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndId(UUID cartId, UUID itemId);

    /** Legacy — used for individual-unit items (tier is null) */
    Optional<CartItem> findByCartIdAndProductIdAndTierIsNull(UUID cartId, UUID productId);

    /** Collection items — unique per cart + product + tier combination */
    Optional<CartItem> findByCartIdAndProductIdAndTierId(UUID cartId, UUID productId, UUID tierId);

    /** Keep old method for merge logic compatibility */
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    @Modifying
    void deleteByCartId(UUID cartId);
}