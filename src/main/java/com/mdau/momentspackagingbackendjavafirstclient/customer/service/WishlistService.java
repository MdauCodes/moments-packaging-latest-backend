package com.mdau.momentspackagingbackendjavafirstclient.customer.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.customer.entity.Wishlist;
import com.mdau.momentspackagingbackendjavafirstclient.customer.repository.WishlistRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository  productRepository;
    private final ProductService     productService;

    @Transactional(readOnly = true)
    public List<ProductDto> getWishlist(User customer) {
        return wishlistRepository.findByCustomer(customer).stream()
                .map(w -> productService.getById(w.getProduct().getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWishlist(User customer, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));

        if (!wishlistRepository.existsByCustomerAndProduct(customer, product)) {
            wishlistRepository.save(Wishlist.builder()
                    .customer(customer).product(product).build());
        }
    }

    @Transactional
    public void removeFromWishlist(User customer, UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + productId));
        wishlistRepository.deleteByCustomerAndProduct(customer, product);
    }
}