package com.mdau.momentspackagingbackendjavafirstclient.product.service;

import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.common.exception.ResourceNotFoundException;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatus;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ReviewCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ReviewDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Review;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ReviewRepository;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository  reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository   orderRepository;

    @Transactional
    public ReviewDto createReview(User customer, ReviewCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCustomer() == null ||
                !order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Order does not belong to this customer");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "You can only review products from delivered orders");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (reviewRepository.existsByCustomerAndProductIdAndOrderId(
                customer, product.getId(), order.getId())) {
            throw new IllegalArgumentException(
                    "You have already reviewed this product for this order");
        }

        Review review = Review.builder()
                .customer(customer).product(product)
                .orderId(order.getId())
                .rating(request.getRating())
                .comment(request.getComment())
                .verified(true)
                .build();

        return new ReviewDto(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> getProductReviews(UUID productId, Pageable pageable) {
        return new PageResponse<>(
                reviewRepository.findByProductId(productId, pageable)
                        .map(ReviewDto::new));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRatingSummary(UUID productId) {
        Double average = reviewRepository.getAverageRatingByProductId(productId);
        long   count   = reviewRepository.countByProductId(productId);
        return Map.of(
                "average", average != null ? Math.round(average * 10.0) / 10.0 : 0.0,
                "count",   count);
    }
}