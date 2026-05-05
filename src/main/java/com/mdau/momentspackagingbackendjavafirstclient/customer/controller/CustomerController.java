package com.mdau.momentspackagingbackendjavafirstclient.customer.controller;

import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.CustomerProfileDto;
import com.mdau.momentspackagingbackendjavafirstclient.auth.dto.CustomerProfileUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.auth.service.CustomerService;
import com.mdau.momentspackagingbackendjavafirstclient.customer.service.WishlistService;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService  customerService;
    private final WishlistService  wishlistService;

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileDto> getProfile(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerService.getProfile(user.getId()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<CustomerProfileDto> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(user.getId(), request));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<ProductDto>> getWishlist(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishlistService.getWishlist(user));
    }

    @PostMapping("/wishlist/{productId}")
    public ResponseEntity<Void> addToWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable UUID productId) {
        wishlistService.addToWishlist(user, productId);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable UUID productId) {
        wishlistService.removeFromWishlist(user, productId);
        return ResponseEntity.noContent().build();
    }
}