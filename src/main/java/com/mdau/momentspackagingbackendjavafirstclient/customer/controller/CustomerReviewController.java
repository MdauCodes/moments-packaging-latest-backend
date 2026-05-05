package com.mdau.momentspackagingbackendjavafirstclient.customer.controller;

import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ReviewCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ReviewDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ReviewService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/reviews")
@RequiredArgsConstructor
public class CustomerReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.status(201)
                .body(reviewService.createReview(user, request));
    }
}