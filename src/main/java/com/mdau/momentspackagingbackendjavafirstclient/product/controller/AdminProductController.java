package com.mdau.momentspackagingbackendjavafirstclient.product.controller;

import com.mdau.momentspackagingbackendjavafirstclient.audit.service.AuditLogService;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.annotation.IsStaffOrAdmin;
import com.mdau.momentspackagingbackendjavafirstclient.common.dto.PageResponse;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductCreateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductDto;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.ProductUpdateRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.dto.StockAdjustRequest;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.InventoryService;
import com.mdau.momentspackagingbackendjavafirstclient.product.service.ProductService;
import com.mdau.momentspackagingbackendjavafirstclient.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AuditLogService  auditLogService;

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
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        ProductDto created = productService.createProduct(request);
        auditLogService.log(actor, "PRODUCT", created.getId().toString(), created.getName(),
                "CREATE", null, null, httpRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {

        ProductDto before = productService.getById(id);
        ProductDto updated = productService.updateProduct(id, request);

        // Build changes JSON — include price change detail if prices changed
        String changesJson = buildPriceChangesJson(before, updated, request);
        String action = request.getBasePrice() != null || request.getOriginalBasePrice() != null
                || Boolean.TRUE.equals(request.getClearOriginalBasePrice())
                ? "PRICE_CHANGE" : "UPDATE";

        auditLogService.log(actor, "PRODUCT", id.toString(), updated.getName(),
                action, null, changesJson, httpRequest);

        return ResponseEntity.ok(updated);
    }

    @IsAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        ProductDto product = productService.getById(id);
        productService.deleteProduct(id);
        auditLogService.log(actor, "PRODUCT", id.toString(), product.getName(),
                "DELETE", null, null, httpRequest);
        return ResponseEntity.noContent().build();
    }

    // -- Inventory endpoints --------------------------------------------------

    @IsStaffOrAdmin
    @PostMapping("/{id}/stock/adjust")
    public ResponseEntity<ProductDto> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockAdjustRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        ProductDto updated = inventoryService.adjustStock(id, request);
        auditLogService.log(actor, "PRODUCT", id.toString(), updated.getName(),
                "STOCK_ADJUST",
                request.getReason(),
                "{\"delta\":" + request.getDelta() + ",\"type\":\"" + request.getType() + "\"}",
                httpRequest);
        return ResponseEntity.ok(updated);
    }

    @IsStaffOrAdmin
    @PutMapping("/{id}/stock/set")
    public ResponseEntity<ProductDto> setStock(
            @PathVariable UUID id,
            @RequestParam int count,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        ProductDto updated = inventoryService.setStock(id, count, reason);
        auditLogService.log(actor, "PRODUCT", id.toString(), updated.getName(),
                "STOCK_SET", reason, "{\"newCount\":" + count + "}", httpRequest);
        return ResponseEntity.ok(updated);
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

    private String buildPriceChangesJson(ProductDto before, ProductDto after,
                                          ProductUpdateRequest request) {
        StringBuilder sb = new StringBuilder("{");
        if (request.getBasePrice() != null) {
            sb.append("\"basePrice\":{\"from\":")
              .append(before.getBasePrice())
              .append(",\"to\":")
              .append(after.getBasePrice())
              .append("},");
        }
        if (request.getOriginalBasePrice() != null
                || Boolean.TRUE.equals(request.getClearOriginalBasePrice())) {
            sb.append("\"originalBasePrice\":{\"from\":")
              .append(before.getOriginalBasePrice())
              .append(",\"to\":")
              .append(after.getOriginalBasePrice())
              .append("},");
        }
        String result = sb.toString();
        if (result.endsWith(",")) result = result.substring(0, result.length() - 1);
        return result + "}";
    }
}