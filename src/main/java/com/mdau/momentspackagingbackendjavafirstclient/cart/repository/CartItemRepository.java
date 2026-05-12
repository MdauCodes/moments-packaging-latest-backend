package com.mdau.momentspackagingbackendjavafirstclient.cart.repository;

import com.mdau.momentspackagingbackendjavafirstclient.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndId(UUID cartId, UUID itemId);
    Optional<CartItem> findByCartIdAndProductIdAndTierIsNull(UUID cartId, UUID productId);
    Optional<CartItem> findByCartIdAndProductIdAndTierId(UUID cartId, UUID productId, UUID tierId);
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    @Modifying
    void deleteByCartId(UUID cartId);

    /** Step 1 fix: delete all cart items referencing a product before deleting the product */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.product.id = :productId")
    void deleteByProductId(UUID productId);

    /** Step 2 fix: null out tier FK on cart items when tiers are being replaced during product update */
    @Modifying
    @Query("UPDATE CartItem ci SET ci.tier = null, ci.collectionNameSnapshot = null, ci.collectionQuantitySnapshot = null WHERE ci.product.id = :productId AND ci.tier IS NOT NULL")
    void nullifyTiersByProductId(UUID productId);
}