package com.mdau.momentspackagingbackendjavafirstclient.product.dto;

import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Review;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ReviewDto {
    private final UUID    id;
    private final String  customerName;
    private final Integer rating;
    private final String  comment;
    private final Boolean verified;
    private final Instant createdAt;

    public ReviewDto(Review review) {
        this.id           = review.getId();
        this.customerName = review.getCustomer().getFirstName();
        this.rating       = review.getRating();
        this.comment      = review.getComment();
        this.verified     = review.getVerified();
        this.createdAt    = review.getCreatedAt();
    }
}