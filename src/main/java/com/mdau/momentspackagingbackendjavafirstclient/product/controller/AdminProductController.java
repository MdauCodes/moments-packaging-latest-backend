package com.mdau.momentspackagingbackendjavafirstclient.product.controller;

import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.StockAdjustRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.InventoryService;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService   productService;
    private final InventoryService inventoryService;

    @IsStaffOrAdmin
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

    @IsStaffOrAdmin
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @IsStaffOrAdmin
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductDto created = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // -- Inventory endpoints ----------------------------------------------------

    @IsStaffOrAdmin
    @PostMapping("/{id}/stock/adjust")
    public ResponseEntity<ProductDto> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAdjustRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(id, request));
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}/stock/set")
    public ResponseEntity<ProductDto> setStock(
            @PathVariable UUID id,
            @RequestParam int count,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(inventoryService.setStock(id, count, reason));
    }

    @IsStaffOrAdmin
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStockProducts());
    }

    @IsStaffOrAdmin
    @GetMapping("/inventory/out-of-stock")
    public ResponseEntity<List<ProductDto>> getOutOfStock() {
        return ResponseEntity.ok(inventoryService.getOutOfStockProducts());
    }
}