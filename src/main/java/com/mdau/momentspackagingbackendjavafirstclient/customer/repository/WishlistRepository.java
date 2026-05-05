package com.mdau.momentspackagingbackendjavafirstclient.customer.repository;

import com.mdau.momentspackagingbackendjavafirstclient.customer.entity.Wishlist;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByCustomer(User customer);
    Optional<Wishlist> findByCustomerAndProduct(User customer, Product product);
    boolean existsByCustomerAndProduct(User customer, Product product);
    void deleteByCustomerAndProduct(User customer, Product product);
}