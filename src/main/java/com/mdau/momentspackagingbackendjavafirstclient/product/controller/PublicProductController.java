package com.mdau.momentspackagingbackendjavafirstclient.product.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.config.RateLimitConfig;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductSearchService;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/products")
@RequiredArgsConstructor
public class PublicProductController {

    private final ProductService       productService;
    private final ProductSearchService productSearchService;
    private final RateLimitConfig      rateLimitConfig;

    @GetMapping
    public ResponseEntity<PageResponse<ProductDto>> getProducts(
            @RequestParam(required = false) UUID    industryId,
            @RequestParam(required = false) Boolean isDiscount,
            @RequestParam(required = false) Boolean isNewArrival,
            @RequestParam(required = false) Boolean isFastMoving,
            @RequestParam(required = false) String  category,
            @PageableDefault(size = 20, sort = "createdAt",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(new PageResponse<>(
                productService.getProducts(industryId, isDiscount, isNewArrival,
                        isFastMoving, category, pageable)));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<ProductDto>> getRecommended() {
        return ResponseEntity.ok(productService.getRecommended());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDto>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(productSearchService.search(q, limit));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDto> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    @PostMapping("/{id}/click")
    public ResponseEntity<Void> recordClick(@PathVariable UUID id,
                                             HttpServletRequest request) {
        rateLimitConfig.checkClick(request);
        productService.recordClick(id);
        return ResponseEntity.noContent().build();
    }
}